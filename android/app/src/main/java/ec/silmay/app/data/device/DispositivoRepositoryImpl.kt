package ec.silmay.app.data.device

import android.content.SharedPreferences
import androidx.core.content.edit
import ec.silmay.app.domain.repository.DispositivoRepository
import java.util.UUID

/**
 * `deviceId` estable por instalación (docs/firebase-schema.md §1).
 *
 * Se genera un UUID v4 la primera vez y se persiste en preferencias privadas. No se usa
 * `ANDROID_ID` ni identificadores de hardware (privacidad). Como el respaldo y la
 * transferencia entre dispositivos están excluidos (RNF-05), un terminal nuevo o una
 * reinstalación obtienen un `deviceId` nuevo, que es el comportamiento deseado.
 */
class DispositivoRepositoryImpl(
    private val preferencias: SharedPreferences
) : DispositivoRepository {

    companion object {
        const val CLAVE_DEVICE_ID = "device_id"
    }

    @Synchronized
    override fun obtenerDeviceId(): String {
        preferencias.getString(CLAVE_DEVICE_ID, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val nuevo = UUID.randomUUID().toString()
        // commit() síncrono: el ID debe estar en disco antes de firmar el primer registro.
        preferencias.edit(commit = true) { putString(CLAVE_DEVICE_ID, nuevo) }
        return nuevo
    }
}
