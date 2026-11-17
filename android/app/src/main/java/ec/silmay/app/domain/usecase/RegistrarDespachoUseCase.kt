package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Despacho
import ec.silmay.app.domain.model.EstadoValidacion
import ec.silmay.app.domain.model.MetodoCobro
import ec.silmay.app.domain.model.UnidadMedida
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.DespachoRepository
import ec.silmay.app.domain.repository.DispositivoRepository
import ec.silmay.app.domain.repository.JornadaRepository
import ec.silmay.app.domain.sync.GeneradorUuid
import ec.silmay.app.domain.sync.Reloj

/**
 * Parámetros para el registro de una venta y despacho de material pétreo (HU-03).
 */
data class RegistrarDespachoParams(
    val materialId: String,
    val unidadMedida: UnidadMedida,
    val cantidad: Double,
    val clienteId: String,
    val volquetaId: String,
    val lugarId: String? = null,
    val metodoCobro: MetodoCobro,
    val valorUSD: Double,
    val fechaHoraMillis: Long? = null
)

/**
 * Caso de Uso: Registro de venta y despacho de pétreos (HU-03).
 *
 * Validaciones:
 * - cantidad > 0 (RF-12).
 * - valorUSD > 0 (MSG-07).
 * - Hereda automáticamente el frente de extracción (lugarId) de la jornada activa (HU-03 CA1).
 * - Genera UUID v4 criptográfico y almacena en disco con estadoValidacion = 'pendiente' (CA1, CA4).
 * - Control de excepciones de almacenamiento lleno (MSG-04) sin descartar datos digitados (CA3).
 *
 * Puro Kotlin (Clean Architecture estricta).
 */
class RegistrarDespachoUseCase(
    private val authRepository: AuthRepository,
    private val dispositivoRepository: DispositivoRepository,
    private val jornadaRepository: JornadaRepository,
    private val despachoRepository: DespachoRepository,
    private val generadorUuid: GeneradorUuid,
    private val reloj: Reloj
) {
    suspend operator fun invoke(params: RegistrarDespachoParams): AppResult<Despacho> {
        val sesion = authRepository.obtenerSesionActual()
            ?: return AppResult.Error(AppMessage.MSG_18)

        // Validación de campos requeridos
        if (params.materialId.isBlank() || params.clienteId.isBlank() || params.volquetaId.isBlank() || params.cantidad <= 0.0) {
            return AppResult.Error(AppMessage.MSG_18)
        }

        // Validación de valor monetario (MSG-07)
        if (params.valorUSD <= 0.0) {
            return AppResult.Error(AppMessage.MSG_07)
        }

        // HU-03 CA1: Herencia de lugar de la jornada activa
        val lugarFinal = if (!params.lugarId.isNullOrBlank()) {
            params.lugarId.trim()
        } else {
            val jornadaActivaResult = jornadaRepository.obtenerJornadaAbierta(sesion.uid)
            val jornadaActiva = (jornadaActivaResult as? AppResult.Success)?.data
            jornadaActiva?.lugarId ?: return AppResult.Error(AppMessage.MSG_18)
        }

        val deviceId = dispositivoRepository.obtenerDeviceId()
        val fechaHora = params.fechaHoraMillis ?: reloj.ahoraMillis()
        val uuid = generadorUuid.nuevo()

        val nuevoDespacho = Despacho(
            uuid = uuid,
            materialId = params.materialId.trim(),
            unidadMedida = params.unidadMedida,
            cantidad = params.cantidad,
            clienteId = params.clienteId.trim(),
            volquetaId = params.volquetaId.trim(),
            lugarId = lugarFinal,
            metodoCobro = params.metodoCobro,
            valorUSD = params.valorUSD,
            fechaHoraMillis = fechaHora,
            estadoValidacion = EstadoValidacion.PENDIENTE,
            validadoPor = null,
            fechaValidacionMillis = null,
            comentarioValidacion = null,
            userId = sesion.uid,
            deviceId = deviceId,
            zona = sesion.zonaAsignada,
            createdAtClientMillis = reloj.ahoraMillis()
        )

        return when (val guardado = despachoRepository.guardarDespacho(nuevoDespacho)) {
            is AppResult.Success -> AppResult.Success(nuevoDespacho)
            is AppResult.Error -> AppResult.Error(guardado.message, guardado.throwable)
        }
    }
}
