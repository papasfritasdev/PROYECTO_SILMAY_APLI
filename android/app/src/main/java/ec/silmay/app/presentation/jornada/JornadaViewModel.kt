package ec.silmay.app.presentation.jornada

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Deduccion
import ec.silmay.app.domain.model.ItemCatalogo
import ec.silmay.app.domain.model.Jornada
import ec.silmay.app.domain.model.Maquinaria
import ec.silmay.app.domain.model.TipoCatalogo
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.CatalogoRepository
import ec.silmay.app.domain.repository.JornadaRepository
import ec.silmay.app.domain.usecase.CerrarJornadaParams
import ec.silmay.app.domain.usecase.CerrarJornadaUseCase
import ec.silmay.app.domain.usecase.IniciarJornadaParams
import ec.silmay.app.domain.usecase.IniciarJornadaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JornadaUiState(
    val cargando: Boolean = true,
    val jornadaActiva: Jornada? = null,
    val maquinasDisponibles: List<Maquinaria> = emptyList(),
    val lugaresDisponibles: List<ItemCatalogo> = emptyList(),
    val motivosDeduccion: List<ItemCatalogo> = emptyList(),
    val maquinaSeleccionada: Maquinaria? = null,
    val lugarSeleccionado: ItemCatalogo? = null,
    val horometroInicial: String = "",
    val horometroFinal: String = "",
    val horaSalidaMillis: Long = System.currentTimeMillis(),
    val observaciones: String = "",
    val deducciones: List<Deduccion> = emptyList(),
    val errorMensaje: AppMessage? = null,
    val operacionExitosa: Boolean = false
)

@HiltViewModel
class JornadaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val catalogoRepository: CatalogoRepository,
    private val jornadaRepository: JornadaRepository,
    private val iniciarJornadaUseCase: IniciarJornadaUseCase,
    private val cerrarJornadaUseCase: CerrarJornadaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JornadaUiState())
    val uiState: StateFlow<JornadaUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, errorMensaje = null) }
            val sesion = authRepository.obtenerSesionActual()
            if (sesion == null) {
                _uiState.update { it.copy(cargando = false, errorMensaje = AppMessage.MSG_18) }
                return@launch
            }

            // Consultar si el operador ya tiene una jornada activa abierta
            val abiertaResult = jornadaRepository.obtenerJornadaAbierta(sesion.uid)
            val abierta = (abiertaResult as? AppResult.Success)?.data

            val maquinasResult = catalogoRepository.obtenerMaquinaria(soloOperativas = true)
            val maquinas = (maquinasResult as? AppResult.Success)?.data ?: emptyList()

            val lugaresResult = catalogoRepository.obtenerItems(TipoCatalogo.LUGARES, soloActivos = true)
            val lugares = (lugaresResult as? AppResult.Success)?.data ?: emptyList()

            val motivosResult = catalogoRepository.obtenerItems(TipoCatalogo.MOTIVOS, soloActivos = true)
            val motivos = (motivosResult as? AppResult.Success)?.data ?: emptyList()

            _uiState.update { state ->
                state.copy(
                    cargando = false,
                    jornadaActiva = abierta,
                    maquinasDisponibles = maquinas,
                    lugaresDisponibles = lugares,
                    motivosDeduccion = motivos,
                    maquinaSeleccionada = if (abierta == null) maquinas.firstOrNull() else null,
                    lugarSeleccionado = if (abierta == null) lugares.firstOrNull() else null,
                    horometroInicial = if (abierta == null && maquinas.isNotEmpty()) {
                        maquinas.first().horometroActual.toString()
                    } else ""
                )
            }
        }
    }

    fun alSeleccionarMaquina(maquina: Maquinaria) {
        viewModelScope.launch {
            _uiState.update { it.copy(maquinaSeleccionada = maquina) }
            val horometroResult = jornadaRepository.obtenerUltimoHorometroMaquina(maquina.codigoInterno)
            val horometro = (horometroResult as? AppResult.Success)?.data ?: maquina.horometroActual
            _uiState.update { it.copy(horometroInicial = horometro.toString()) }
        }
    }

    fun alSeleccionarLugar(lugar: ItemCatalogo) {
        _uiState.update { it.copy(lugarSeleccionado = lugar) }
    }

    fun alCambiarHorometroInicial(valor: String) {
        _uiState.update { it.copy(horometroInicial = valor) }
    }

    fun alCambiarHorometroFinal(valor: String) {
        _uiState.update { it.copy(horometroFinal = valor) }
    }

    fun alCambiarHoraSalida(millis: Long) {
        _uiState.update { it.copy(horaSalidaMillis = millis) }
    }

    fun alCambiarObservaciones(valor: String) {
        _uiState.update { it.copy(observaciones = valor) }
    }

    fun agregarDeduccion(deduccion: Deduccion) {
        _uiState.update { it.copy(deducciones = it.deducciones + deduccion) }
    }

    fun quitarDeduccion(id: String) {
        _uiState.update { it.copy(deducciones = it.deducciones.filterNot { d -> d.id == id }) }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(errorMensaje = null, operacionExitosa = false) }
    }

    fun iniciarJornada(onExito: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val maquina = state.maquinaSeleccionada
            val lugar = state.lugarSeleccionado
            val horometro = state.horometroInicial.toDoubleOrNull()

            if (maquina == null || lugar == null || horometro == null || horometro < 0.0) {
                _uiState.update { it.copy(errorMensaje = AppMessage.MSG_18) }
                return@launch
            }

            _uiState.update { it.copy(cargando = true, errorMensaje = null) }
            val resultado = iniciarJornadaUseCase(
                IniciarJornadaParams(
                    maquinaId = maquina.codigoInterno,
                    lugarId = lugar.id,
                    horometroInicial = horometro
                )
            )

            when (resultado) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            jornadaActiva = resultado.data,
                            operacionExitosa = true
                        )
                    }
                    onExito()
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(cargando = false, errorMensaje = resultado.message)
                    }
                }
            }
        }
    }

    fun cerrarJornada(onExito: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val jornada = state.jornadaActiva
            val horometro = state.horometroFinal.toDoubleOrNull()

            if (jornada == null || horometro == null) {
                _uiState.update { it.copy(errorMensaje = AppMessage.MSG_05) }
                return@launch
            }

            _uiState.update { it.copy(cargando = true, errorMensaje = null) }
            val resultado = cerrarJornadaUseCase(
                CerrarJornadaParams(
                    jornadaUuid = jornada.uuid,
                    horaSalidaMillis = state.horaSalidaMillis,
                    horometroFinal = horometro,
                    observaciones = state.observaciones,
                    deducciones = state.deducciones
                )
            )

            when (resultado) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            jornadaActiva = null,
                            operacionExitosa = true
                        )
                    }
                    onExito()
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(cargando = false, errorMensaje = resultado.message)
                    }
                }
            }
        }
    }
}
