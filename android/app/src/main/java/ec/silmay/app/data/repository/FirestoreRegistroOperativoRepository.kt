package ec.silmay.app.data.repository

import android.database.sqlite.SQLiteFullException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import ec.silmay.app.data.sync.BitacoraPendientes
import ec.silmay.app.data.sync.ConciliadorSincronizacion
import ec.silmay.app.data.sync.PendienteSync
import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ColeccionOperativa
import ec.silmay.app.domain.model.MetadatosControl
import ec.silmay.app.domain.repository.RegistroOperativoRepository
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException

/**
 * Persistencia local-primero de registros operativos sobre la caché en disco del SDK de
 * Firestore (HT-07 §1–§3, ADR-0004, ADR-0005).
 *
 * - Escribe siempre con `document(uuid).set(...)`: un reenvío del SDK sobrescribe el mismo
 *   documento con datos idénticos y nunca duplica (RNF-11).
 * - No espera el `Task` de `set()`, que solo se completa con el acuse del servidor. La
 *   persistencia en disco se confirma con una lectura de caché: la cola interna del SDK
 *   procesa las operaciones en orden, así que la lectura ve la escritura ya aplicada
 *   (RNF-01, ≤ 2 s sin red).
 * - La mutación queda en la cola persistente del SDK, que sobrevive a cierres forzados y
 *   reinicios y se transmite en orden FIFO con backoff al recuperar la red.
 */
class FirestoreRegistroOperativoRepository(
    private val firestore: FirebaseFirestore,
    private val bitacora: BitacoraPendientes,
    private val conciliador: ConciliadorSincronizacion
) : RegistroOperativoRepository {

    override suspend fun guardar(
        coleccion: ColeccionOperativa,
        metadatos: MetadatosControl,
        datos: Map<String, Any?>
    ): AppResult<String> {
        val referencia = firestore.collection(coleccion.valor).document(metadatos.uuid)
        val documento = construirDocumento(metadatos, datos)

        val escritura = referencia.set(documento)
        return try {
            val enCache = referencia.get(Source.CACHE).await()
            if (!enCache.exists()) {
                return AppResult.Error(AppMessage.MSG_18)
            }

            bitacora.agregar(PendienteSync(metadatos.userId, coleccion.valor, metadatos.uuid))
            conciliador.solicitar()
            AppResult.Success(metadatos.uuid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Si la escritura local falló, su excepción explica la causa real.
            val causa = escritura.takeIf { it.isComplete }?.exception ?: e
            AppResult.Error(mensajePara(causa), causa)
        }
    }

    companion object {
        /** Documento final: datos de negocio + los 6 metadatos de control (firebase-schema §1). */
        fun construirDocumento(
            metadatos: MetadatosControl,
            datos: Map<String, Any?>
        ): Map<String, Any?> {
            val documento = LinkedHashMap<String, Any?>(datos)
            documento[MetadatosControl.UUID] = metadatos.uuid
            documento[MetadatosControl.CREATED_AT_CLIENT] =
                Timestamp(Date(metadatos.createdAtClientMillis))
            documento[MetadatosControl.SYNCED_AT_SERVER] = FieldValue.serverTimestamp()
            documento[MetadatosControl.USER_ID] = metadatos.userId
            documento[MetadatosControl.DEVICE_ID] = metadatos.deviceId
            documento[MetadatosControl.ZONA] = metadatos.zona
            return documento
        }

        /** MSG-04 si el disco está lleno; MSG-18 para cualquier otro fallo local. */
        fun mensajePara(error: Throwable): AppMessage {
            var actual: Throwable? = error
            while (actual != null) {
                if (actual is SQLiteFullException) return AppMessage.MSG_04
                if (actual is IOException &&
                    actual.message?.contains("No space left", ignoreCase = true) == true
                ) {
                    return AppMessage.MSG_04
                }
                actual = actual.cause.takeIf { it !== actual }
            }
            return AppMessage.MSG_18
        }
    }
}
