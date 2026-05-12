package com.javiermontillaarias.escapemanager.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javiermontillaarias.escapemanager.data.repository.AuthRepository
import com.javiermontillaarias.escapemanager.util.Resource
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<Unit>>()
    val loginState: LiveData<Resource<Unit>> = _loginState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = Resource.Error("Completa todos los campos")
            return
        }
        viewModelScope.launch {
            _loginState.value = Resource.Loading
            _loginState.value = repository.login(email, password)
        }
    }
}