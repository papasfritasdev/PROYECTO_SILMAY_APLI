package ec.silmay.app.domain.repository

/** Operaciones auditables del contrato `registrarRechazo` (docs/cloud-functions.md §2.3). */
enum class OperacionRechazada(val valor: String) {
    READ("read"),
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete")
}

/**
 * Registro de rechazos PERMISSION_DENIED en `auditoria` (HT-07 §4). De mejor esfuerzo:
 * nunca lanza; devuelve `false` si la auditoría no pudo registrarse.
 */
interface AuditoriaRechazosRepository {
    suspend fun registrarRechazo(
        coleccion: String,
        documentoId: String,
        operacion: OperacionRechazada
    ): Boolean
}
