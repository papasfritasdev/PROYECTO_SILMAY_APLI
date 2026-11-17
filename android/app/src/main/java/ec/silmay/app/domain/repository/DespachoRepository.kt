package ec.silmay.app.domain.repository

import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Despacho

/**
 * Contrato de Dominio para la persistencia y consulta offline de despachos (HU-03).
 * Cero dependencias de Android SDK o Firebase SDK (Clean Architecture estricta).
 */
interface DespachoRepository {
    /**
     * Guarda un despacho de material pétreo en disco de forma local-primero.
     * Retorna el UUID del despacho persistido.
     */
    suspend fun guardarDespacho(despacho: Despacho): AppResult<String>

    /**
     * Obtiene los despachos registrados por el usuario en el dispositivo.
     */
    suspend fun listarDespachosPorUsuario(userId: String): AppResult<List<Despacho>>

    /**
     * Obtiene un despacho específico por su UUID único.
     */
    suspend fun obtenerDespachoPorUuid(uuid: String): AppResult<Despacho?>
}
