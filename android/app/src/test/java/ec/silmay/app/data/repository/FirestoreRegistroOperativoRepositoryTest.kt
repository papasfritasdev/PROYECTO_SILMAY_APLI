package ec.silmay.app.data.repository

import android.database.sqlite.SQLiteFullException
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import ec.silmay.app.data.sync.BitacoraPendientes
import ec.silmay.app.data.sync.ConciliadorSincronizacion
import ec.silmay.app.data.sync.PendienteSync
import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ColeccionOperativa
import ec.silmay.app.domain.model.MetadatosControl
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Date

/** HT-07 §1–§3: escritura idempotente con set(uuid) confirmada en disco sin esperar a la red. */
class FirestoreRegistroOperativoRepositoryTest {

    private val metadatos = MetadatosControl(
        uuid = "3f2b8c1e-5d4a-4b6c-9e7f-1a2b3c4d5e6f",
        createdAtClientMillis = 1_762_000_000_000L,
        userId = "uid-op",
        deviceId = "device-1",
        zona = "cantera-banos"
    )

    private lateinit var firestore: FirebaseFirestore
    private lateinit var coleccion: CollectionReference
    private lateinit var documento: DocumentReference
    private lateinit var cache: DocumentSnapshot
    private lateinit var bitacora: BitacoraPendientes
    private lateinit var conciliador: ConciliadorSincronizacion
    private lateinit var repo: FirestoreRegistroOperativoRepository

    /** Simula el SDK sin red: el Task de set() nunca se completa (espera el acuse del servidor). */
    private val escrituraSinAcuse = TaskCompletionSource<Void>().task

    @Before
    fun setUp() {
        firestore = mockk()
        coleccion = mockk()
        documento = mockk()
        cache = mockk()
        bitacora = mockk(relaxed = true)
        conciliador = mockk(relaxed = true)

        every { firestore.collection("gastos") } returns coleccion
        every { coleccion.document(metadatos.uuid) } returns documento
        every { documento.set(any()) } returns escrituraSinAcuse
        every { documento.get(Source.CACHE) } returns Tasks.forResult(cache)
        every { cache.exists() } returns true

        repo = FirestoreRegistroOperativoRepository(firestore, bitacora, conciliador)
    }

    @Test
    fun `escribe con document(uuid) y set, sin esperar el acuse del servidor`() = runTest {
        val resultado = repo.guardar(ColeccionOperativa.GASTOS, metadatos, mapOf("rubro" to "combustible"))

        assertEquals(AppResult.Success(metadatos.uuid), resultado)
        verify(exactly = 1) { coleccion.document(metadatos.uuid) }
        verify(exactly = 0) { coleccion.add(any()) }
        // La confirmación local es una lectura de caché encolada después de la escritura.
        verifyOrder {
            documento.set(any())
            documento.get(Source.CACHE)
        }
    }

    @Test
    fun `el documento lleva los 6 metadatos de control con los tipos del esquema`() = runTest {
        val escrito = slot<Any>()
        every { documento.set(capture(escrito)) } returns escrituraSinAcuse

        repo.guardar(ColeccionOperativa.GASTOS, metadatos, mapOf("rubro" to "combustible", "montoUSD" to 12.5))

        @Suppress("UNCHECKED_CAST")
        val doc = escrito.captured as Map<String, Any?>
        assertEquals("combustible", doc["rubro"])
        assertEquals(12.5, doc["montoUSD"])
        assertEquals(metadatos.uuid, doc["uuid"])
        assertEquals(Timestamp(Date(metadatos.createdAtClientMillis)), doc["createdAtClient"])
        assertEquals(FieldValue.serverTimestamp(), doc["syncedAtServer"])
        assertEquals("uid-op", doc["userId"])
        assertEquals("device-1", doc["deviceId"])
        assertEquals("cantera-banos", doc["zona"])
    }

    @Test
    fun `registra el pendiente en la bitacora y solicita conciliacion`() = runTest {
        repo.guardar(ColeccionOperativa.GASTOS, metadatos, emptyMap())

        verifyOrder {
            bitacora.agregar(PendienteSync("uid-op", "gastos", metadatos.uuid))
            conciliador.solicitar()
        }
    }

    @Test
    fun `si el documento no quedo en cache no se confirma ni se encola en bitacora`() = runTest {
        every { cache.exists() } returns false

        val resultado = repo.guardar(ColeccionOperativa.GASTOS, metadatos, emptyMap())

        assertEquals(AppMessage.MSG_18, (resultado as AppResult.Error).message)
        verify(exactly = 0) { bitacora.agregar(any()) }
    }

    @Test
    fun `disco lleno durante la escritura local devuelve MSG-04`() = runTest {
        val discoLleno = mockk<SQLiteFullException>(relaxed = true)
        every { documento.set(any()) } returns Tasks.forException(RuntimeException("write", discoLleno))
        every { documento.get(Source.CACHE) } returns Tasks.forException(
            mockk<FirebaseFirestoreException>(relaxed = true)
        )

        val resultado = repo.guardar(ColeccionOperativa.GASTOS, metadatos, emptyMap())

        assertEquals(AppMessage.MSG_04, (resultado as AppResult.Error).message)
        verify(exactly = 0) { bitacora.agregar(any()) }
    }

    @Test
    fun `otros fallos locales devuelven MSG-18 con la causa original`() = runTest {
        val error = IllegalStateException("cliente terminado")
        every { documento.get(Source.CACHE) } returns Tasks.forException(error)

        val resultado = repo.guardar(ColeccionOperativa.GASTOS, metadatos, emptyMap()) as AppResult.Error

        assertEquals(AppMessage.MSG_18, resultado.message)
        assertSame(error, resultado.throwable)
    }

    @Test
    fun `mapeo de errores de almacenamiento`() {
        assertEquals(
            AppMessage.MSG_04,
            FirestoreRegistroOperativoRepository.mensajePara(IOException("write failed: ENOSPC (No space left on device)"))
        )
        assertEquals(AppMessage.MSG_18, FirestoreRegistroOperativoRepository.mensajePara(IOException("otro")))
        assertTrue(FirestoreRegistroOperativoRepository.mensajePara(RuntimeException()) == AppMessage.MSG_18)
    }
}
