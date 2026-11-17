package ec.silmay.app.domain.repository

/**
 * Contrato de Dominio para el estado de sincronización y control de cola offline (HU-07, HU-08).
 * Permite conocer los registros pendientes de subir a la nube para proteger la sesión del usuario.
 * Cero dependencias de Android SDK o Firebase SDK (Clean Architecture estricta).
 */
interface SincronizacionRepository {
    /**
     * Retorna la cantidad de registros operativos pendientes de confirmación en servidor
     * para el usuario especificado.
     */
    suspend fun contarPendientes(userId: String): Int

    /**
     * Indica si el usuario tiene registros pendientes de subir a la nube.
     */
    suspend fun tienePendientes(userId: String): Boolean = contarPendientes(userId) > 0
}
