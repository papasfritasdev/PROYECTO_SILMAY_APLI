package ec.silmay.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.usecase.IniciarSesionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la gestión del inicio de sesión (M7 / HU-01a).
 * Expone un StateFlow inmutable consumido reactivamente por Jetpack Compose.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val iniciarSesionUseCase: IniciarSesionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(nuevoEmail: String) {
        _uiState.update { it.copy(email = nuevoEmail, mensajeError = null) }
    }

    fun onContrasenaChanged(nuevaContrasena: String) {
        _uiState.update { it.copy(contrasena = nuevaContrasena, mensajeError = null) }
    }

    fun iniciarSesion() {
        val currentState = _uiState.value
        if (!currentState.puedeIniciarSesion) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, mensajeError = null) }

            when (val resultado = iniciarSesionUseCase(currentState.email, currentState.contrasena)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sesionActiva = resultado.data,
                            mensajeError = null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mensajeError = resultado.message,
                            sesionActiva = null
                        )
                    }
                }
            }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(mensajeError = null) }
    }
}
