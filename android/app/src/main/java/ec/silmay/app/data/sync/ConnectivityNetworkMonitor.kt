package ec.silmay.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import ec.silmay.app.domain.repository.NetworkMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Implementación de [NetworkMonitor] basada en ConnectivityManager.NetworkCallback (HU-08 CA1).
 * Detecta de forma reactiva transiciones de red celular/Wi-Fi sin polling ni gasto innecesario de batería.
 */
class ConnectivityNetworkMonitor(
    context: Context
) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    override val esOnline: Flow<Boolean> = callbackFlow {
        val cm = connectivityManager
        if (cm == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            private val redesActivas = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                redesActivas.add(network)
                trySend(true)
            }

            override fun onLost(network: Network) {
                redesActivas.remove(network)
                trySend(redesActivas.isNotEmpty())
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, callback)

        // Emitir estado inicial puntual
        trySend(estaConectado())

        awaitClose {
            cm.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged().conflate()

    override fun estaConectado(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
