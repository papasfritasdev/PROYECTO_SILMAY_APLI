package ec.silmay.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de Dominio para el monitoreo reactivo del estado de conectividad a Internet (HU-08).
 * Cero dependencias de Android SDK (Clean Architecture estricta).
 */
interface NetworkMonitor {
    /**
     * Flujo reactivo que emite `true` cuando el dispositivo tiene conexión activa
     * a Internet (Wi-Fi, 3G, 4G, 5G), o `false` en modo avión / sin cobertura.
     */
    val esOnline: Flow<Boolean>

    /**
     * Consulta síncrona puntual del estado actual de conectividad.
     */
    fun estaConectado(): Boolean
}
