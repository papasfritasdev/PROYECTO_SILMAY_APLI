package ec.silmay.app.data.repository

import ec.silmay.app.data.sync.BitacoraPendientes
import ec.silmay.app.domain.repository.SincronizacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación del contrato de sincronización basada en la bitácora local en disco (HU-07, HU-08).
 * Consulta y observa reactivamente los registros operativos pendientes de confirmación en el servidor.
 */
class SincronizacionRepositoryImpl(
    private val bitacora: BitacoraPendientes
) : SincronizacionRepository {

    override suspend fun contarPendientes(userId: String): Int =
        bitacora.listar().count { it.userId == userId }

    override fun observarPendientes(userId: String): Flow<Int> =
        bitacora.observar().map { lista -> lista.count { it.userId == userId } }
}
