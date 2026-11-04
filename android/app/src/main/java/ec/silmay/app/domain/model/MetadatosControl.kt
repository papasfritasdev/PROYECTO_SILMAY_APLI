package ec.silmay.app.domain.model

/**
 * Los 6 metadatos obligatorios de control de cada registro operativo (HT-07 §2,
 * docs/firebase-schema.md §1). `syncedAtServer` no se representa aquí: lo asigna el
 * servidor (FieldValue.serverTimestamp()) y la capa de datos lo inyecta al escribir.
 *
 * Puro Kotlin.
 */
data class MetadatosControl(
    val uuid: String,
    val createdAtClientMillis: Long,
    val userId: String,
    val deviceId: String,
    val zona: String
) {
    companion object {
        const val UUID = "uuid"
        const val CREATED_AT_CLIENT = "createdAtClient"
        const val SYNCED_AT_SERVER = "syncedAtServer"
        const val USER_ID = "userId"
        const val DEVICE_ID = "deviceId"
        const val ZONA = "zona"

        /** Claves reservadas: el llamador nunca puede fijarlas (ni suplantar autor o zona). */
        val CAMPOS_CONTROL: Set<String> =
            setOf(UUID, CREATED_AT_CLIENT, SYNCED_AT_SERVER, USER_ID, DEVICE_ID, ZONA)
    }
}
