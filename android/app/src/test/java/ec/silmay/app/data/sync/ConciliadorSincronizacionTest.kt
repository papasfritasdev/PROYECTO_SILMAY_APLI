package ec.silmay.app.data.sync

import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import ec.silmay.app.domain.repository.AuditoriaRechazosRepository
import ec.silmay.app.domain.repository.OperacionRechazada
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** HT-07 §4: detección y auditoría de rechazos PERMISSION_DENIED tras vaciar la cola FIFO. */
class ConciliadorSincronizacionTest {

    private val aceptado = PendienteSync("uid-op", "gastos", "11111111-1111-4111-8111-111111111111")
    private val rechazado = PendienteSync("uid-op", "jornadas", "22222222-2222-4222-a222-222222222222")
    private val deOtroUsuario = PendienteSync("uid-otro", "gastos", "33333333-3333-4333-8333-333333333333")

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var bitacora: BitacoraPendientes
    private lateinit var auditoria: AuditoriaRechazosRepository
    private val documentos = mutableMapOf<String, DocumentReference>()

    @Before
    fun setUp() {
        firestore = mockk()
        auth = mockk()
        auditoria = mockk()
        bitacora = BitacoraPendientesPreferencias(PreferenciasEnMemoria())

        val usuario = mockk<FirebaseUser>()
        every { usuario.uid } returns "uid-op"
        every { auth.currentUser } returns usuario
        every { firestore.waitForPendingWrites() } returns Tasks.forResult(null)
        coEvery { auditoria.registrarRechazo(any(), any(), any()) } returns true

        listOf("gastos", "jornadas").forEach { nombre ->
            val coleccion = mockk<CollectionReference>()
            every { firestore.collection(nombre) } returns coleccion
            every { coleccion.document(any()) } answers {
                documentos.getOrPut("$nombre/${firstArg<String>()}") { mockk() }
            }
        }
    }

    private fun doc(p: PendienteSync) = firestore.collection(p.coleccion).document(p.uuid)

    private fun enServidor(p: PendienteSync, existe: Boolean) {
        val snapshot = mockk<DocumentSnapshot>()
        every { snapshot.exists() } returns existe
        every { doc(p).get(Source.SERVER) } returns Tasks.forResult(snapshot)
    }

    /** Errores simulados: el enum `Code` real no carga en la JVM (usa SparseArray). */
    private class PermisoDenegado : Exception()
    private class NoDisponible : Exception()

    private fun lecturaFalla(p: PendienteSync, error: Exception) {
        every { doc(p).get(Source.SERVER) } returns Tasks.forException(error)
    }

    private fun TestScope.conciliador() =
        ConciliadorSincronizacion(firestore, auth, bitacora, auditoria, backgroundScope) { it is PermisoDenegado }

    @Test
    fun `aceptado se retira sin auditar y denegado se audita como create`() = runTest {
        bitacora.agregar(aceptado)
        bitacora.agregar(rechazado)
        enServidor(aceptado, existe = true)
        lecturaFalla(rechazado, PermisoDenegado())

        val quedan = conciliador().conciliar()

        assertFalse(quedan)
        assertTrue(bitacora.listar().isEmpty())
        coVerify(exactly = 1) {
            auditoria.registrarRechazo("jornadas", rechazado.uuid, OperacionRechazada.CREATE)
        }
        coVerify(exactly = 0) { auditoria.registrarRechazo("gastos", any(), any()) }
    }

    @Test
    fun `documento inexistente en servidor tras vaciar la cola es un rechazo`() = runTest {
        bitacora.agregar(rechazado)
        enServidor(rechazado, existe = false)

        conciliador().conciliar()

        coVerify { auditoria.registrarRechazo("jornadas", rechazado.uuid, OperacionRechazada.CREATE) }
        assertTrue(bitacora.listar().isEmpty())
    }

    @Test
    fun `error transitorio deja el pendiente para reintentar`() = runTest {
        bitacora.agregar(aceptado)
        lecturaFalla(aceptado, NoDisponible())

        val quedan = conciliador().conciliar()

        assertTrue(quedan)
        assertEquals(listOf(aceptado), bitacora.listar())
        coVerify(exactly = 0) { auditoria.registrarRechazo(any(), any(), any()) }
    }

    @Test
    fun `si la auditoria falla el rechazo se conserva para reintentar`() = runTest {
        bitacora.agregar(rechazado)
        lecturaFalla(rechazado, PermisoDenegado())
        coEvery { auditoria.registrarRechazo(any(), any(), any()) } returns false

        assertTrue(conciliador().conciliar())
        assertEquals(listOf(rechazado), bitacora.listar())
    }

    @Test
    fun `solo concilia pendientes del usuario activo`() = runTest {
        bitacora.agregar(deOtroUsuario)

        assertFalse(conciliador().conciliar())

        verify(exactly = 0) { firestore.waitForPendingWrites() }
        assertEquals(listOf(deOtroUsuario), bitacora.listar())
    }

    @Test
    fun `sin sesion no concilia`() = runTest {
        every { auth.currentUser } returns null
        bitacora.agregar(aceptado)

        assertFalse(conciliador().conciliar())
        verify(exactly = 0) { firestore.waitForPendingWrites() }
    }

    @Test
    fun `espera a que el SDK vacie la cola antes de leer del servidor`() = runTest {
        val colaPendiente = TaskCompletionSource<Void>()
        every { firestore.waitForPendingWrites() } returns colaPendiente.task
        bitacora.agregar(aceptado)
        enServidor(aceptado, existe = true)

        val trabajo = async { conciliador().conciliar() }
        runCurrent()
        verify(exactly = 0) { doc(aceptado).get(Source.SERVER) }

        colaPendiente.setResult(null)
        assertFalse(trabajo.await())
        verify(exactly = 1) { doc(aceptado).get(Source.SERVER) }
    }

    @Test
    fun `iniciar registra un listener de sesion que dispara la conciliacion`() = runTest {
        val listener = io.mockk.slot<FirebaseAuth.AuthStateListener>()
        every { auth.addAuthStateListener(capture(listener)) } returns Unit
        bitacora.agregar(aceptado)
        enServidor(aceptado, existe = true)

        val conciliador = conciliador()
        conciliador.iniciar()
        conciliador.iniciar() // idempotente
        verify(exactly = 1) { auth.addAuthStateListener(any()) }

        listener.captured.onAuthStateChanged(auth)
        runCurrent()

        assertTrue(bitacora.listar().isEmpty())
    }
}
