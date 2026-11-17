package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.EstadoJornada
import ec.silmay.app.domain.model.Jornada
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.DispositivoRepository
import ec.silmay.app.domain.repository.JornadaRepository
import ec.silmay.app.domain.sync.GeneradorUuid
import ec.silmay.app.domain.sync.Reloj

/**
 * Parámetros para la apertura de una jornada de maquinaria (HU-04 CA1).
 */
data class IniciarJornadaParams(
    val maquinaId: String,
    val lugarId: String,
    val horometroInicial: Double,
    val horaInicioMillis: Long? = null
)

/**
 * Caso de Uso: Apertura de jornada de trabajo en maquinaria pesada (HU-04 CA1, CA4).
 *
 * Reglas de negocio:
 * - Restricción de jornada única abierta por operador (MSG-14).
 * - Generación de UUID v4 y registro de metadatos de control (userId, deviceId, zona, createdAtClient).
 * - Estado inicial 'abierta', procesada = false, tieneFallaCritica = false.
 *
 * Puro Kotlin (Clean Architecture estricta).
 */
class IniciarJornadaUseCase(
    private val authRepository: AuthRepository,
    private val dispositivoRepository: DispositivoRepository,
    private val jornadaRepository: JornadaRepository,
    private val generadorUuid: GeneradorUuid,
    private val reloj: Reloj
) {
    suspend operator fun invoke(params: IniciarJornadaParams): AppResult<Jornada> {
        val sesion = authRepository.obtenerSesionActual()
            ?: return AppResult.Error(AppMessage.MSG_18)

        if (params.maquinaId.isBlank() || params.lugarId.isBlank() || params.horometroInicial < 0.0) {
            return AppResult.Error(AppMessage.MSG_18)
        }

        // HU-04 CA4: Restricción de jornada única abierta por operador (MSG-14)
        val abiertaExistente = jornadaRepository.obtenerJornadaAbierta(sesion.uid)
        if (abiertaExistente is AppResult.Success && abiertaExistente.data != null) {
            return AppResult.Error(AppMessage.MSG_14)
        }

        val deviceId = dispositivoRepository.obtenerDeviceId()
        val horaInicio = params.horaInicioMillis ?: reloj.ahoraMillis()
        val uuid = generadorUuid.nuevo()

        val nuevaJornada = Jornada(
            uuid = uuid,
            maquinaId = params.maquinaId.trim(),
            lugarId = params.lugarId.trim(),
            horaInicioMillis = horaInicio,
            horometroInicial = params.horometroInicial,
            estado = EstadoJornada.ABIERTA,
            tieneFallaCritica = false,
            notificadaFalla = false,
            procesada = false,
            userId = sesion.uid,
            deviceId = deviceId,
            zona = sesion.zonaAsignada,
            createdAtClientMillis = reloj.ahoraMillis()
        )

        return when (val guardado = jornadaRepository.guardarJornada(nuevaJornada)) {
            is AppResult.Success -> AppResult.Success(nuevaJornada)
            is AppResult.Error -> AppResult.Error(guardado.message, guardado.throwable)
        }
    }
}
