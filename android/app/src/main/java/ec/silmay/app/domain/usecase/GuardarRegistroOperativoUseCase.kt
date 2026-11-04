package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ColeccionOperativa
import ec.silmay.app.domain.model.MetadatosControl
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.DispositivoRepository
import ec.silmay.app.domain.repository.RegistroOperativoRepository
import ec.silmay.app.domain.sync.GeneradorUuid
import ec.silmay.app.domain.sync.Reloj
import ec.silmay.app.domain.sync.UuidV4

/**
 * Caso de Uso: guardar un registro operativo nuevo en disco (HT-07 §1–§2, RNF-11).
 *
 * Inyecta los metadatos de control a partir de la sesión persistida (funciona sin red):
 * `uuid` (UUID v4 nuevo), `createdAtClient`, `userId`, `deviceId`, `zona` y, en la capa de
 * datos, `syncedAtServer`. Las claves de control que traiga el llamador se descartan para
 * que nadie pueda fijar el autor, la zona o el ID.
 *
 * Puro Kotlin (Clean Architecture estricta).
 */
class GuardarRegistroOperativoUseCase(
    private val authRepository: AuthRepository,
    private val dispositivoRepository: DispositivoRepository,
    private val registroOperativoRepository: RegistroOperativoRepository,
    private val generadorUuid: GeneradorUuid,
    private val reloj: Reloj
) {
    suspend operator fun invoke(
        coleccion: ColeccionOperativa,
        datos: Map<String, Any?>
    ): AppResult<String> {
        val sesion = authRepository.obtenerSesionActual()
            ?: return AppResult.Error(AppMessage.MSG_18)

        if (!sesion.estado || !sesion.rol.habilitadoEnMovil) {
            return AppResult.Error(AppMessage.MSG_13)
        }
        if (sesion.zonaAsignada.isBlank()) {
            return AppResult.Error(AppMessage.MSG_18)
        }

        val uuid = generadorUuid.nuevo()
        if (!UuidV4.esValido(uuid)) {
            return AppResult.Error(AppMessage.MSG_18)
        }

        val deviceId = dispositivoRepository.obtenerDeviceId()
        if (deviceId.isBlank()) {
            return AppResult.Error(AppMessage.MSG_18)
        }

        val metadatos = MetadatosControl(
            uuid = uuid,
            createdAtClientMillis = reloj.ahoraMillis(),
            userId = sesion.uid,
            deviceId = deviceId,
            zona = sesion.zonaAsignada
        )
        val datosDeNegocio = datos.filterKeys { it !in MetadatosControl.CAMPOS_CONTROL }

        return registroOperativoRepository.guardar(coleccion, metadatos, datosDeNegocio)
    }
}
