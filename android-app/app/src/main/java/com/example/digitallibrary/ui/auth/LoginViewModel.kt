package com.example.digitallibrary.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitallibrary.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val pass: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _uiState.update { it.copy(email = newEmail, errorMessage = null) }
    }

    fun onPasswordChanged(newPass: String) {
        _uiState.update { it.copy(pass = newPass, errorMessage = null) }
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val pass = _uiState.value.pass

        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter both email and password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.login(email, pass)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Login failed. Please check your credentials."
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
            }
        }
    }
}
