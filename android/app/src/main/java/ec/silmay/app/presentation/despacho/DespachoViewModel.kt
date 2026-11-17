package ec.silmay.app.presentation.despacho

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ItemCatalogo
import ec.silmay.app.domain.model.Jornada
import ec.silmay.app.domain.model.Maquinaria
import ec.silmay.app.domain.model.MetodoCobro
import ec.silmay.app.domain.model.TipoCatalogo
import ec.silmay.app.domain.model.UnidadMedida
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.CatalogoRepository
import ec.silmay.app.domain.repository.JornadaRepository
import ec.silmay.app.domain.usecase.RegistrarDespachoParams
import ec.silmay.app.domain.usecase.RegistrarDespachoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DespachoUiState(
    val cargando: Boolean = true,
    val jornadaActiva: Jornada? = null,
    val materiales: List<ItemCatalogo> = emptyList(),
    val clientes: List<ItemCatalogo> = emptyList(),
    val volquetas: List<Maquinaria> = emptyList(),
    val lugares: List<ItemCatalogo> = emptyList(),
    val materialSeleccionado: ItemCatalogo? = null,
    val unidadMedida: UnidadMedida = UnidadMedida.M3,
    val cantidad: String = "",
    val clienteSeleccionado: ItemCatalogo? = null,
    val volquetaSeleccionada: Maquinaria? = null,
    val lugarSeleccionado: ItemCatalogo? = null,
    val metodoCobro: MetodoCobro = MetodoCobro.EFECTIVO,
    val valorUSD: String = "",
    val errorMensaje: AppMessage? = null,
    val errorCamposIncompletos: Boolean = false,
    val operacionExitosa: Boolean = false
)

@HiltViewModel
class DespachoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val catalogoRepository: CatalogoRepository,
    private val jornadaRepository: JornadaRepository,
    private val registrarDespachoUseCase: RegistrarDespachoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DespachoUiState())
    val uiState: StateFlow<DespachoUiState> = _uiState.asStateFlow()

    init {
        cargarCatalogos()
    }

    fun cargarCatalogos() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, errorMensaje = null) }
            val sesion = authRepository.obtenerSesionActual()
            if (sesion == null) {
                _uiState.update { it.copy(cargando = false, errorMensaje = AppMessage.MSG_18) }
                return@launch
            }

            // Jornada activa para heredar lugarId
            val jornadaResult = jornadaRepository.obtenerJornadaAbierta(sesion.uid)
            val jornadaActiva = (jornadaResult as? AppResult.Success)?.data

            // Materiales
            val materialesResult = catalogoRepository.obtenerItems(TipoCatalogo.MATERIALES, soloActivos = true)
            val materiales = (materialesResult as? AppResult.Success)?.data ?: emptyList()

            // Clientes
            val clientesResult = catalogoRepository.obtenerItems(TipoCatalogo.CLIENTES, soloActivos = true)
            val clientes = (clientesResult as? AppResult.Success)?.data ?: emptyList()

            // Maquinaria (filtrar volquetas)
            val maquinasResult = catalogoRepository.obtenerMaquinaria(soloOperativas = true)
            val todasMaquinas = (maquinasResult as? AppResult.Success)?.data ?: emptyList()
            val volquetas = todasMaquinas.filter { it.esVolqueta }

            // Lugares
            val lugaresResult = catalogoRepository.obtenerItems(TipoCatalogo.LUGARES, soloActivos = true)
            val lugares = (lugaresResult as? AppResult.Success)?.data ?: emptyList()

            val lugarHeredado = if (jornadaActiva != null) {
                lugares.find { it.id == jornadaActiva.lugarId } ?: ItemCatalogo(jornadaActiva.lugarId, jornadaActiva.lugarId)
            } else {
                lugares.firstOrNull()
            }

            _uiState.update { state ->
                state.copy(
                    cargando = false,
                    jornadaActiva = jornadaActiva,
                    materiales = materiales,
                    clientes = clientes,
                    volquetas = volquetas,
                    lugares = lugares,
                    materialSeleccionado = materiales.firstOrNull(),
                    clienteSeleccionado = clientes.firstOrNull(),
                    volquetaSeleccionada = volquetas.firstOrNull(),
                    lugarSeleccionado = lugarHeredado
                )
            }
        }
    }

    fun alSeleccionarMaterial(item: ItemCatalogo) {
        _uiState.update { it.copy(materialSeleccionado = item) }
    }

    fun alSeleccionarUnidad(unidad: UnidadMedida) {
        _uiState.update { it.copy(unidadMedida = unidad) }
    }

    fun alCambiarCantidad(valor: String) {
        _uiState.update { it.copy(cantidad = valor, errorCamposIncompletos = false) }
    }

    fun alSeleccionarCliente(item: ItemCatalogo) {
        _uiState.update { it.copy(clienteSeleccionado = item) }
    }

    fun alSeleccionarVolqueta(item: Maquinaria) {
        _uiState.update { it.copy(volquetaSeleccionada = item) }
    }

    fun alSeleccionarLugar(item: ItemCatalogo) {
        _uiState.update { it.copy(lugarSeleccionado = item) }
    }

    fun alSeleccionarMetodoCobro(metodo: MetodoCobro) {
        _uiState.update { it.copy(metodoCobro = metodo) }
    }

    fun alCambiarValorUSD(valor: String) {
        _uiState.update { it.copy(valorUSD = valor, errorCamposIncompletos = false) }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(errorMensaje = null, errorCamposIncompletos = false, operacionExitosa = false) }
    }

    fun guardarDespacho(alertaHaptica: () -> Unit = {}, onExito: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val material = state.materialSeleccionado
            val cliente = state.clienteSeleccionado
            val volqueta = state.volquetaSeleccionada
            val cantidad = state.cantidad.toDoubleOrNull()
            val valor = state.valorUSD.toDoubleOrNull()
            val lugarId = state.jornadaActiva?.lugarId ?: state.lugarSeleccionado?.id

            // HU-03 CA2: Validación local inmediata con aviso háptico
            if (material == null || cliente == null || volqueta == null || lugarId.isNullOrBlank() ||
                cantidad == null || cantidad <= 0.0 || valor == null || valor <= 0.0) {
                _uiState.update {
                    it.copy(
                        errorCamposIncompletos = true,
                        errorMensaje = if (valor != null && valor <= 0.0) AppMessage.MSG_07 else null
                    )
                }
                alertaHaptica()
                return@launch
            }

            _uiState.update { it.copy(cargando = true, errorMensaje = null, errorCamposIncompletos = false) }

            val params = RegistrarDespachoParams(
                materialId = material.id,
                unidadMedida = state.unidadMedida,
                cantidad = cantidad,
                clienteId = cliente.id,
                volquetaId = volqueta.codigoInterno,
                lugarId = lugarId,
                metodoCobro = state.metodoCobro,
                valorUSD = valor
            )

            when (val resultado = registrarDespachoUseCase(params)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            operacionExitosa = true,
                            cantidad = "",
                            valorUSD = ""
                        )
                    }
                    onExito()
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(cargando = false, errorMensaje = resultado.message)
                    }
                    alertaHaptica()
                }
            }
        }
    }
}
