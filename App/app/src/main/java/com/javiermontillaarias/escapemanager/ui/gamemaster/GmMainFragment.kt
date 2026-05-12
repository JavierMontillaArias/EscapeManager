package com.javiermontillaarias.escapemanager.ui.gamemaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.javiermontillaarias.escapemanager.R
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import com.javiermontillaarias.escapemanager.databinding.FragmentGmMainBinding
import com.javiermontillaarias.escapemanager.ui.gamemaster.dashboard.GmDashboardFragment
import com.javiermontillaarias.escapemanager.ui.gamemaster.incidents.CreateIncidentFragment
import com.javiermontillaarias.escapemanager.ui.gamemaster.qrscanner.QrScannerFragment

class GmMainFragment : Fragment() {

    private var _binding: FragmentGmMainBinding? = null
    private val binding get() = _binding!!
    private val sessionManager by lazy { SessionManager(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGmMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nombre del usuario en el toolbar
        binding.toolbar.title = "Hola, ${sessionManager.userName ?: "Game Master"}"

        // Tab inicial
        if (savedInstanceState == null) {
            showFragment(GmDashboardFragment())
            binding.bottomNav.selectedItemId = R.id.nav_gm_dashboard
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_gm_dashboard -> showFragment(GmDashboardFragment())
                R.id.nav_scan_qr -> showFragment(QrScannerFragment())
                R.id.nav_gm_incidents -> showFragment(CreateIncidentFragment())
            }
            true
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun showFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}