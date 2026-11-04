package ec.silmay.app.data.repository

import com.google.firebase.functions.FirebaseFunctions
import ec.silmay.app.domain.repository.AuditoriaRechazosRepository
import ec.silmay.app.domain.repository.OperacionRechazada
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Audita rechazos PERMISSION_DENIED mediante la callable `registrarRechazo`
 * (docs/cloud-functions.md §2.3). El autor lo toma el servidor del token; aquí solo
 * se envían colección, documento, operación y `origen: 'android'`.
 */
@Singleton
class FunctionsAuditoriaRechazosRepository @Inject constructor(
    private val functions: FirebaseFunctions
) : AuditoriaRechazosRepository {

    companion object {
        const val CALLABLE = "registrarRechazo"
        const val ORIGEN = "android"
    }

    override suspend fun registrarRechazo(
        coleccion: String,
        documentoId: String,
        operacion: OperacionRechazada
    ): Boolean {
        return try {
            functions.getHttpsCallable(CALLABLE)
                .call(
                    mapOf(
                        "coleccion" to coleccion,
                        "documentoId" to documentoId,
                        "operacion" to operacion.valor,
                        "origen" to ORIGEN
                    )
                )
                .await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }
}
