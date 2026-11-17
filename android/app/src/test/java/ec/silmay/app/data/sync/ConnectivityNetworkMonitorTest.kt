package ec.silmay.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConnectivityNetworkMonitorTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var monitor: ConnectivityNetworkMonitor

    @Before
    fun setUp() {
        context = mockk()
        connectivityManager = mockk()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        monitor = ConnectivityNetworkMonitor(context)
    }

    @Test
    fun `estaConectado retorna true cuando la red activa tiene NET_CAPABILITY_INTERNET`() {
        val network: Network = mockk()
        val capabilities: NetworkCapabilities = mockk()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        assertTrue(monitor.estaConectado())
    }

    @Test
    fun `estaConectado retorna false cuando no hay red activa`() {
        every { connectivityManager.activeNetwork } returns null

        assertFalse(monitor.estaConectado())
    }

    @Test
    fun `estaConectado retorna false cuando la red no tiene capability de internet`() {
        val network: Network = mockk()
        val capabilities: NetworkCapabilities = mockk()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false

        assertFalse(monitor.estaConectado())
    }
}
