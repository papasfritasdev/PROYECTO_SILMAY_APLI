package ec.silmay.app.data.sync

import android.content.SharedPreferences
import androidx.core.content.edit

/** Registro escrito en este dispositivo cuya aceptación por el servidor aún no se verificó. */
data class PendienteSync(
    val userId: String,
    val coleccion: String,
    val uuid: String
) {
    fun serializar(): String = "$userId$SEPARADOR$coleccion$SEPARADOR$uuid"

    companion object {
        private const val SEPARADOR = "|"

        fun deserializar(valor: String): PendienteSync? {
            val partes = valor.split(SEPARADOR)
            if (partes.size != 3 || partes.any { it.isBlank() }) return null
            return PendienteSync(partes[0], partes[1], partes[2])
        }
    }
}

/**
 * Bitácora en disco de los registros pendientes de verificación (HT-07 §4).
 *
 * La cola de mutaciones en sí la persiste y transmite en orden FIFO el SDK de Firestore
 * (ADR-0004); esta bitácora solo recuerda qué documentos hay que verificar para detectar
 * rechazos PERMISSION_DENIED aunque la app se cierre o el dispositivo se reinicie antes
 * de que el servidor responda.
 */
interface BitacoraPendientes {
    fun agregar(pendiente: PendienteSync)
    fun quitar(pendiente: PendienteSync)
    fun listar(): List<PendienteSync>
}

class BitacoraPendientesPreferencias(
    private val preferencias: SharedPreferences
) : BitacoraPendientes {

    companion object {
        const val CLAVE_PENDIENTES = "pendientes"
    }

    @Synchronized
    override fun agregar(pendiente: PendienteSync) {
        guardar(leer() + pendiente.serializar())
    }

    @Synchronized
    override fun quitar(pendiente: PendienteSync) {
        guardar(leer() - pendiente.serializar())
    }

    @Synchronized
    override fun listar(): List<PendienteSync> =
        leer().mapNotNull { PendienteSync.deserializar(it) }

    // getStringSet devuelve una instancia interna que no debe modificarse: siempre se copia.
    private fun leer(): Set<String> =
        preferencias.getStringSet(CLAVE_PENDIENTES, emptySet())?.toSet() ?: emptySet()

    // commit() síncrono para sobrevivir a un cierre forzado inmediatamente posterior.
    private fun guardar(valores: Set<String>) {
        preferencias.edit(commit = true) { putStringSet(CLAVE_PENDIENTES, valores) }
    }
}
