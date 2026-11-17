package ec.silmay.app.data.repository

import ec.silmay.app.data.sync.BitacoraPendientes
import ec.silmay.app.data.sync.PendienteSync
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SincronizacionRepositoryImplTest {

    private val bitacora: BitacoraPendientes = mockk()
    private val flow = MutableStateFlow<List<PendienteSync>>(emptyList())
    private lateinit var repo: SincronizacionRepositoryImpl

    @Before
    fun setUp() {
        every { bitacora.observar() } returns flow
        repo = SincronizacionRepositoryImpl(bitacora)
    }

    @Test
    fun `contarPendientes filtra solo los registros del usuario indicado`() = runTest {
        val pendientes = listOf(
            PendienteSync("usr-1", "jornadas", "uuid-1"),
            PendienteSync("usr-1", "despachos", "uuid-2"),
            PendienteSync("usr-2", "jornadas", "uuid-3")
        )
        every { bitacora.listar() } returns pendientes

        val conteo = repo.contarPendientes("usr-1")

        assertEquals(2, conteo)
        assertTrue(repo.tienePendientes("usr-1"))
        assertFalse(repo.tienePendientes("usr-3"))
    }

    @Test
    fun `observarPendientes emite reactivamente cuando la bitacora cambia`() = runTest {
        flow.value = listOf(
            PendienteSync("usr-1", "jornadas", "uuid-1")
        )

        val primerValor = repo.observarPendientes("usr-1").first()
        assertEquals(1, primerValor)
    }
}
