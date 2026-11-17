package ec.silmay.app.data.repository

import ec.silmay.app.data.sync.BitacoraPendientes
import ec.silmay.app.domain.repository.SincronizacionRepository

/**
 * Implementación del contrato de sincronización basada en la bitácora local en disco (HU-07, HU-08).
 * Consulta los registros operativos pendientes de confirmación en el servidor.
 */
class SincronizacionRepositoryImpl(
    private val bitacora: BitacoraPendientes
) : SincronizacionRepository {

    override suspend fun contarPendientes(userId: String): Int =
        bitacora.listar().count { it.userId == userId }
}
