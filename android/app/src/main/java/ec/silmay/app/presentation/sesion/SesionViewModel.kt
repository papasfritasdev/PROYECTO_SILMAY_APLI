package ec.silmay.app.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ec.silmay.app.data.sync.ConciliadorSincronizacion
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.NetworkMonitor
import ec.silmay.app.domain.repository.SincronizacionRepository
import ec.silmay.app.domain.usecase.CerrarSesionResultado
import ec.silmay.app.domain.usecase.CerrarSesionUseCase
import ec.silmay.app.domain.usecase.ObtenerSesionActualUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    private val cerrarSesionUseCase: CerrarSesionUseCase,
    private val networkMonitor: NetworkMonitor,
    private val sincronizacionRepository: SincronizacionRepository,
    private val conciliador: ConciliadorSincronizacion
) : ViewModel() {

    private val _uiState = MutableStateFlow<SesionUiState>(SesionUiState.Verificando)
    val uiState: StateFlow<SesionUiState> = _uiState.asStateFlow()

    private val _alertaPendientes = MutableStateFlow<Int?>(null)
    val alertaPendientes: StateFlow<Int?> = _alertaPendientes.asStateFlow()

    val esOnline: StateFlow<Boolean> = networkMonitor.esOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkMonitor.estaConectado())

    private val _pendientesSync = MutableStateFlow(0)
    val pendientesSync: StateFlow<Int> = _pendientesSync.asStateFlow()

    fun limpiarAlertaPendientes() {
        _alertaPendientes.value = null
    }

    init {
        viewModelScope.launch {
            val sesion = obtenerSesionActual()
            _uiState.value = sesion?.let { SesionUiState.Autenticada(it) } ?: SesionUiState.SinSesion
            if (sesion != null) {
                sincronizacionRepository.observarPendientes(sesion.uid).collect {
                    _pendientesSync.value = it
                }
            }
        }
    }

    fun onLoginExitoso(sesion: UsuarioSesion) {
        _uiState.value = SesionUiState.Autenticada(sesion)
        viewModelScope.launch {
            sincronizacionRepository.observarPendientes(sesion.uid).collect {
                _pendientesSync.value = it
            }
        }
    }

    fun forzarSincronizacion() {
        conciliador.solicitar()
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            when (val resultado = cerrarSesionUseCase()) {
                is CerrarSesionResultado.Exito -> {
                    _uiState.value = SesionUiState.SinSesion
                }
                is CerrarSesionResultado.BloqueadoPorPendientes -> {
                    _alertaPendientes.value = resultado.cantidadPendientes
                }
                is CerrarSesionResultado.Error -> {
                    // Si ocurre un error inesperado, no alteramos la sesión local para proteger al usuario.
                }
            }
        }
    }
}
