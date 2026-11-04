package ec.silmay.app.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import ec.silmay.app.domain.repository.AuditoriaRechazosRepository
import ec.silmay.app.domain.repository.OperacionRechazada
import ec.silmay.app.domain.sync.ResolucionPendiente
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

/**
 * Verifica los registros de la [BitacoraPendientes] cuando el SDK terminó de vaciar su
 * cola FIFO y audita los rechazos de las Security Rules (HT-07 §4).
 *
 * Por qué no basta con escuchar el `Task` de `set()`: ese callback se pierde si la app se
 * cierra antes de que el servidor responda; el SDK reenvía la mutación en la siguiente
 * sesión y la descarta en silencio si es rechazada. Tras `waitForPendingWrites()` (que
 * también abarca escrituras de sesiones anteriores) se lee cada documento desde el
 * servidor: si existe, se aceptó; si no existe o su lectura es denegada (las reglas solo
 * permiten leer registros propios existentes), fue rechazado.
 *
 * Se activa al iniciar sesión (o al restaurarla al abrir la app) y tras cada guardado.
 * Ante errores transitorios reintenta con backoff exponencial.
 */
class ConciliadorSincronizacion(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val bitacora: BitacoraPendientes,
    private val auditoria: AuditoriaRechazosRepository,
    private val alcance: CoroutineScope,
    private val esPermisoDenegado: (Exception) -> Boolean = ::esPermisoDenegadoFirestore
) {
    companion object {
        const val REINTENTO_INICIAL_MS = 30_000L
        const val REINTENTO_MAXIMO_MS = 15 * 60_000L
    }

    private val solicitudes = Channel<Unit>(Channel.CONFLATED)
    private val iniciado = AtomicBoolean(false)

    fun iniciar() {
        if (!iniciado.compareAndSet(false, true)) return

        auth.addAuthStateListener { if (it.currentUser != null) solicitar() }

        alcance.launch {
            var espera = REINTENTO_INICIAL_MS
            for (solicitud in solicitudes) {
                val quedanIndeterminados = try {
                    conciliar()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    true
                }
                if (quedanIndeterminados) {
                    alcance.launch {
                        delay(espera)
                        solicitar()
                    }
                    espera = (espera * 2).coerceAtMost(REINTENTO_MAXIMO_MS)
                } else {
                    espera = REINTENTO_INICIAL_MS
                }
            }
        }
    }

    fun solicitar() {
        solicitudes.trySend(Unit)
    }

    /**
     * Concilia los pendientes del usuario activo. Suspende mientras el SDK tenga
     * mutaciones sin respuesta del servidor (p. ej. sin red).
     *
     * @return `true` si quedaron pendientes sin poder resolverse.
     */
    suspend fun conciliar(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val pendientes = bitacora.listar().filter { it.userId == uid }
        if (pendientes.isEmpty()) return false

        firestore.waitForPendingWrites().await()

        var quedanIndeterminados = false
        for (pendiente in pendientes) {
            when (resolver(pendiente)) {
                ResolucionPendiente.CONFIRMADO -> bitacora.quitar(pendiente)
                ResolucionPendiente.RECHAZADO -> {
                    val auditado = auditoria.registrarRechazo(
                        pendiente.coleccion,
                        pendiente.uuid,
                        OperacionRechazada.CREATE
                    )
                    // Si la auditoría falla (p. ej. se cayó la red), se reintenta luego.
                    if (auditado) bitacora.quitar(pendiente) else quedanIndeterminados = true
                }
                ResolucionPendiente.INDETERMINADO -> quedanIndeterminados = true
            }
        }
        return quedanIndeterminados
    }

    private suspend fun resolver(pendiente: PendienteSync): ResolucionPendiente {
        return try {
            val snapshot = firestore.collection(pendiente.coleccion)
                .document(pendiente.uuid)
                .get(Source.SERVER)
                .await()
            ResolucionPendiente.resolver(existeEnServidor = snapshot.exists(), lecturaDenegada = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResolucionPendiente.resolver(existeEnServidor = null, lecturaDenegada = esPermisoDenegado(e))
        }
    }
}

/** Rechazo de Security Rules. Aislado porque `Code` no puede cargarse en la JVM de pruebas. */
fun esPermisoDenegadoFirestore(error: Exception): Boolean =
    error is FirebaseFirestoreException &&
        error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
