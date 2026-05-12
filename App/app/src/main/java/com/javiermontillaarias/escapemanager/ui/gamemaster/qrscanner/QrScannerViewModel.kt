package com.javiermontillaarias.escapemanager.ui.gamemaster.qrscanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.model.QrValidateResponse
import com.javiermontillaarias.escapemanager.data.repository.GameRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class QrScannerViewModel(private val repository: GameRepository) : ViewModel() {

    private val _scanResult = MutableLiveData<Resource<QrValidateResponse>>(Resource.Idle)
    val scanResult: LiveData<Resource<QrValidateResponse>> = _scanResult

    private var isProcessing = false

    fun validateQr(token: String) {
        if (isProcessing) return
        isProcessing = true
        viewModelScope.launch {
            _scanResult.value = Resource.Loading
            _scanResult.value = repository.validateQr(token)
            isProcessing = false
        }
    }

    fun resetState() {
        _scanResult.value = Resource.Idle
        isProcessing = false
    }
}