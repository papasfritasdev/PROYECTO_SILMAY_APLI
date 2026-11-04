package ec.silmay.app.domain.repository

/** Identificador estable del terminal emisor (`deviceId`, docs/firebase-schema.md §1). */
interface DispositivoRepository {
    fun obtenerDeviceId(): String
}
