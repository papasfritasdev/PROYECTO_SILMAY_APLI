package ec.silmay.app.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.usecase.CerrarSesionUseCase
import ec.silmay.app.domain.usecase.ObtenerSesionActualUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado global de sesión de la app móvil (M7). */
sealed interface SesionUiState {
    data object Verificando : SesionUiState
    data object SinSesion : SesionUiState
    data class Autenticada(val sesion: UsuarioSesion) : SesionUiState
}

/**
 * ViewModel de sesión a nivel de Activity (HU-01a CA1).
 * Al arrancar recupera la sesión persistida en disco para que la app sea operable
 * sin conexión tras el primer inicio de sesión (sync-protocol §5).
 */
@HiltViewModel
class SesionViewModel @Inject constructor(
    private val obtenerSesionActual: ObtenerSesionActualUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SesionUiState>(SesionUiState.Verificando)
    val uiState: StateFlow<SesionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = obtenerSesionActual()
                ?.let { SesionUiState.Autenticada(it) }
                ?: SesionUiState.SinSesion
        }
    }

    fun onLoginExitoso(sesion: UsuarioSesion) {
        _uiState.value = SesionUiState.Autenticada(sesion)
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            cerrarSesionUseCase()
            _uiState.value = SesionUiState.SinSesion
        }
    }
}
