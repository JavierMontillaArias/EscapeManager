package com.javiermontillaarias.escapemanager.ui.gamemaster.qrscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.javiermontillaarias.escapemanager.EscapeManagerApp
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.network.RetrofitClient
import com.javiermontillaarias.escapemanager.data.repository.GameRepository
import com.javiermontillaarias.escapemanager.databinding.FragmentQrScannerBinding
import com.javiermontillaarias.escapemanager.util.Resource
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QrScannerFragment : Fragment() {

    companion object {
        private const val TAG = "QrScannerFragment"
    }

    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QrScannerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // B-03: singleton SessionManager desde la Application
                val sm = (requireActivity().application as EscapeManagerApp).sessionManager
                val api = RetrofitClient.getApiService(sm)
                @Suppress("UNCHECKED_CAST")
                return QrScannerViewModel(GameRepository(api)) as T
            }
        }
    }

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private val barcodeScanner = BarcodeScanning.getClient()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Snackbar.make(binding.root, getString(R.string.camera_permission_required), Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        viewModel.scanResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Idle -> hideStatus()
                is Resource.Loading -> showStatus("Validando QR...", true)
                is Resource.Success -> {
                    hideStatus()
                    val data = state.data
                    val bundle = Bundle().apply {
                        putInt("gameId", data.gameId)
                        putString("groupName", data.nombreGrupo)
                        putString("roomName", data.sala)
                        putString("startTime", data.startTime)
                    }
                    findNavController().navigate(R.id.activeGameFragment, bundle)
                    viewModel.resetState()
                }
                is Resource.Error -> {
                    showStatus("${state.message}", false)
                    binding.root.postDelayed({
                        hideStatus()
                        viewModel.resetState()
                    }, 3000)
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
            }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al vincular casos de uso de la cámara", e)
            Snackbar.make(binding.root, getString(R.string.camera_error), Snackbar.LENGTH_LONG).show()
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    if (barcode.format == Barcode.FORMAT_QR_CODE) {
                        val rawValue = barcode.rawValue ?: continue
                        // B-02: llamada directa a validateQr — viewModelScope usa Main dispatcher,
                        // no necesita runOnUiThread. activity? evita crash si el Fragment se destruyó.
                        activity ?: return@addOnSuccessListener
                        viewModel.validateQr(rawValue)
                        break
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun showStatus(message: String, showProgress: Boolean) {
        binding.statusOverlay.visibility = View.VISIBLE
        binding.tvStatus.text = message
        binding.progressBar.visibility = if (showProgress) View.VISIBLE else View.GONE
    }

    private fun hideStatus() {
        binding.statusOverlay.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            // B-07: desvincular cámara antes de cerrar el executor para evitar frames huérfanos
            cameraProvider?.unbindAll()
            cameraExecutor.shutdownNow()
            barcodeScanner.close()
        } finally {
            _binding = null
        }
    }
}
