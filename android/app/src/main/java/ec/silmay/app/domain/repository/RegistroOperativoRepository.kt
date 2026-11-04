package ec.silmay.app.domain.repository

import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ColeccionOperativa
import ec.silmay.app.domain.model.MetadatosControl

/**
 * Persistencia local-primero de registros operativos (HT-07, sync-protocol §2 etapas 2–3).
 * La implementación debe escribir con `document(metadatos.uuid).set(...)` (nunca `add()`),
 * confirmar la escritura en disco sin esperar a la red y dejarla encolada para el envío FIFO.
 */
interface RegistroOperativoRepository {
    /** @return el UUID del documento cuando quedó persistido en disco. */
    suspend fun guardar(
        coleccion: ColeccionOperativa,
        metadatos: MetadatosControl,
        datos: Map<String, Any?>
    ): AppResult<String>
}
