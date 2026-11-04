package ec.silmay.app.data.sync

import ec.silmay.app.data.device.DispositivoRepositoryImpl
import ec.silmay.app.domain.sync.UuidV4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** deviceId estable y bitácora de pendientes en disco (HT-07 §2 y §4). */
class PersistenciaLocalTest {

    @Test
    fun `deviceId se genera una vez como UUID v4 y se reutiliza`() {
        val prefs = PreferenciasEnMemoria()
        val repo = DispositivoRepositoryImpl(prefs)

        val primero = repo.obtenerDeviceId()

        assertTrue(UuidV4.esValido(primero))
        assertEquals(primero, repo.obtenerDeviceId())
        // Persistido: otra instancia (reinicio de la app) lee el mismo valor.
        assertEquals(primero, DispositivoRepositoryImpl(prefs).obtenerDeviceId())
        assertEquals(1, prefs.commits)
    }

    @Test
    fun `instalaciones distintas obtienen deviceId distintos`() {
        assertNotEquals(
            DispositivoRepositoryImpl(PreferenciasEnMemoria()).obtenerDeviceId(),
            DispositivoRepositoryImpl(PreferenciasEnMemoria()).obtenerDeviceId()
        )
    }

    @Test
    fun `bitacora persiste, lista y retira pendientes con commit sincrono`() {
        val prefs = PreferenciasEnMemoria()
        val a = PendienteSync("uid-1", "gastos", "11111111-1111-4111-8111-111111111111")
        val b = PendienteSync("uid-1", "jornadas", "22222222-2222-4222-a222-222222222222")

        BitacoraPendientesPreferencias(prefs).apply {
            agregar(a)
            agregar(b)
            agregar(a) // idempotente
        }
        val reabierta = BitacoraPendientesPreferencias(prefs)
        assertEquals(setOf(a, b), reabierta.listar().toSet())

        reabierta.quitar(a)
        assertEquals(listOf(b), BitacoraPendientesPreferencias(prefs).listar())
        assertEquals(4, prefs.commits)
    }

    @Test
    fun `entradas corruptas de la bitacora se ignoran`() {
        val prefs = PreferenciasEnMemoria()
        prefs.valores[BitacoraPendientesPreferencias.CLAVE_PENDIENTES] =
            setOf("basura", "a||c", PendienteSync("u", "gastos", "x").serializar())

        assertEquals(listOf(PendienteSync("u", "gastos", "x")), BitacoraPendientesPreferencias(prefs).listar())
        assertNull(PendienteSync.deserializar("u|gastos"))
    }
}
