package com.javiermontillaarias.escapemanager.ui.gamemaster.qrscanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.QrValidateResponse
import com.javiermontillaarias.escapemanager.data.repository.GameRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class QrScannerViewModel(private val repository: GameRepository) : ViewModel() {

    private val _scanResult = MutableLiveData<Resource<QrValidateResponse>>(Resource.Idle)
    val scanResult: LiveData<Resource<QrValidateResponse>> = _scanResult

    // C3: AtomicBoolean evita race condition entre frames de cámara concurrentes
    private val isProcessing = AtomicBoolean(false)

    fun validateQr(token: String) {
        if (!isProcessing.compareAndSet(false, true)) return
        viewModelScope.launch {
            _scanResult.value = Resource.Loading
            _scanResult.value = repository.validateQr(token)
            isProcessing.set(false)
        }
    }

    fun resetState() {
        _scanResult.value = Resource.Idle
        isProcessing.set(false)
    }
}
