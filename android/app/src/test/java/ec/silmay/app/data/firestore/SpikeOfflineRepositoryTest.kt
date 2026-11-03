package ec.silmay.app.data.firestore

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Suite de Pruebas Técnicas del Spike Offline-First (HT-06).
 * Valida los Criterios Técnicos:
 * - RNF-01: Confirmación de guardado local en <= 2.0 segundos.
 * - RNF-10: Lectura local desde disco (Source.CACHE).
 * - RNF-11: Idempotencia estricta mediante UUID v4 y set().
 */
class SpikeOfflineRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectionRef: CollectionReference
    private lateinit var documentRef: DocumentReference
    private lateinit var repository: SpikeOfflineRepository

    @Before
    fun setUp() {
        firestore = mockk(relaxed = true)
        collectionRef = mockk(relaxed = true)
        documentRef = mockk(relaxed = true)

        every { firestore.collection(SpikeOfflineRepository.COLECCION_SPIKE) } returns collectionRef
        every { collectionRef.document(any()) } returns documentRef

        repository = SpikeOfflineRepository(firestore)
    }

    @Test
    fun `RNF-01 - guardado local atómico se completa en menos de 2 segundos`() = runTest {
        val uuid = UUID.randomUUID().toString()
        val datos = mapOf(
            "uuid" to uuid,
            "horometro" to 1250.5,
            "estado" to "abierta"
        )

        every { documentRef.set(datos) } returns Tasks.forResult(null)

        val resultado = repository.ejecutarGuardadoLocal(uuid = uuid, datos = datos)

        assertTrue("El guardado debe ser exitoso", resultado.guardadoExitoso)
        assertTrue("La latencia debe cumplir RNF-01 (<= 2000 ms)", resultado.cumpleLimiteRNF01)
        assertTrue("Tiempo medido debe ser <= 2000 ms", resultado.tiempoMs <= 2000L)
        assertEquals(uuid, resultado.uuid)

        verify(exactly = 1) { collectionRef.document(uuid) }
        verify(exactly = 1) { documentRef.set(datos) }
    }

    @Test
    fun `RNF-11 - idempotencia garantizada al usar set con el mismo UUID v4`() = runTest {
        val uuid = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        val datos = mapOf("uuid" to uuid, "version" to 1)

        every { documentRef.set(datos) } returns Tasks.forResult(null)

        // Simular dos escrituras seguidas con el mismo UUID (ej. reintento de sync)
        val res1 = repository.ejecutarGuardadoLocal(uuid = uuid, datos = datos)
        val res2 = repository.ejecutarGuardadoLocal(uuid = uuid, datos = datos)

        assertTrue(res1.guardadoExitoso)
        assertTrue(res2.guardadoExitoso)
        // Ambas invocaciones deben apuntar exactamente al mismo Document ID
        verify(exactly = 2) { collectionRef.document(uuid) }
        verify(exactly = 2) { documentRef.set(datos) }
    }

    @Test
    fun `RNF-10 - lectura de datos desde cache local Source_CACHE tras corte de red`() = runTest {
        val uuid = "test-offline-uuid"
        val mockSnapshot = mockk<DocumentSnapshot>()
        val datosGuardados = mapOf<String, Any>(
            "uuid" to uuid,
            "maquinaId" to "CAT-320D",
            "horometro" to 1540.2
        )

        every { mockSnapshot.exists() } returns true
        every { mockSnapshot.data } returns datosGuardados
        every { documentRef.get(Source.CACHE) } returns Tasks.forResult(mockSnapshot)

        val resultado = repository.leerDesdeCache(uuid)

        assertNotNull("Los datos deben recuperarse desde la caché local", resultado)
        assertEquals("CAT-320D", resultado?.get("maquinaId"))
        assertEquals(1540.2, resultado?.get("horometro"))

        verify(exactly = 1) { documentRef.get(Source.CACHE) }
    }

    @Test
    fun `simularModoAvion invoca disableNetwork en Firestore`() = runTest {
        every { firestore.disableNetwork() } returns Tasks.forResult(null)

        repository.simularModoAvion()

        verify(exactly = 1) { firestore.disableNetwork() }
    }

    @Test
    fun `restaurarConexion invoca enableNetwork en Firestore`() = runTest {
        every { firestore.enableNetwork() } returns Tasks.forResult(null)

        repository.restaurarConexion()

        verify(exactly = 1) { firestore.enableNetwork() }
    }

    @Test
    fun `cuando la escritura falla retorna cumpleLimiteRNF01 en false`() = runTest {
        val uuid = UUID.randomUUID().toString()
        val datos = mapOf("test" to "error")

        every { documentRef.set(datos) } returns Tasks.forException(RuntimeException("Disco lleno"))

        val resultado = repository.ejecutarGuardadoLocal(uuid = uuid, datos = datos)

        assertFalse("Guardado debe marcarse fallido", resultado.guardadoExitoso)
        assertFalse("No cumple límite al haber fallado", resultado.cumpleLimiteRNF01)
        assertTrue(resultado.mensaje.contains("Disco lleno"))
    }
}
