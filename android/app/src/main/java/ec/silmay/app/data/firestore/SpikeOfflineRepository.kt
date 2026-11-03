package ec.silmay.app.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * Resultado de la prueba técnica de persistencia offline (HT-06 / RNF-01).
 */
data class ResultadoSpikeOffline(
    val uuid: String,
    val tiempoMs: Long,
    val cumpleLimiteRNF01: Boolean, // tiempoMs <= 2000 ms (2.0 s)
    val guardadoExitoso: Boolean,
    val mensaje: String
)

/**
 * Repositorio de Spike Técnico para verificar la Persistencia Offline-First de Firestore en disco (HT-06).
 * Valida la latencia de guardado local (RNF-01 <= 2.0 s) y lectura desde Source.CACHE (RNF-10).
 */
@Singleton
class SpikeOfflineRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        const val COLECCION_SPIKE = "spike_offline"
        const val LIMITE_LATENCIA_MS = 2000L // 2.0 segundos según RNF-01
    }

    suspend fun simularModoAvion() {
        firestore.disableNetwork().await()
    }

    suspend fun restaurarConexion() {
        firestore.enableNetwork().await()
    }

    /**
     * Guarda un registro operativo de forma atómica usando UUID v4 y set() en la caché local.
     * Mide la latencia de confirmación local en milisegundos.
     */
    suspend fun ejecutarGuardadoLocal(
        uuid: String = UUID.randomUUID().toString(),
        datos: Map<String, Any>
    ): ResultadoSpikeOffline {
        var guardadoExitoso = false
        var errorMensaje: String? = null

        val tiempoMs = measureTimeMillis {
            try {
                firestore.collection(COLECCION_SPIKE)
                    .document(uuid)
                    .set(datos)
                    .await()
                guardadoExitoso = true
            } catch (e: Exception) {
                guardadoExitoso = false
                errorMensaje = e.message
            }
        }

        val cumpleLimite = tiempoMs <= LIMITE_LATENCIA_MS && guardadoExitoso

        return ResultadoSpikeOffline(
            uuid = uuid,
            tiempoMs = tiempoMs,
            cumpleLimiteRNF01 = cumpleLimite,
            guardadoExitoso = guardadoExitoso,
            mensaje = if (guardadoExitoso) {
                "Guardado local confirmado en ${tiempoMs} ms (Límite RNF-01: ${LIMITE_LATENCIA_MS} ms)"
            } else {
                "Error en guardado local: $errorMensaje"
            }
        )
    }

    /**
     * Recupera el documento forzando la lectura exclusiva desde la caché persistente en disco (Source.CACHE).
     * Simula la lectura tras pérdida de red o reinicio del proceso.
     */
    suspend fun leerDesdeCache(uuid: String): Map<String, Any>? {
        return try {
            val snapshot = firestore.collection(COLECCION_SPIKE)
                .document(uuid)
                .get(Source.CACHE)
                .await()

            if (snapshot.exists()) snapshot.data else null
        } catch (_: Exception) {
            null
        }
    }
}
