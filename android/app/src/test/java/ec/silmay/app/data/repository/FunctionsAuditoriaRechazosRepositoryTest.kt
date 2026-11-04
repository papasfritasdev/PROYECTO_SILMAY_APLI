package ec.silmay.app.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import ec.silmay.app.domain.repository.OperacionRechazada
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** HT-07 §4: contrato de la callable registrarRechazo desde Android (cloud-functions §2.3). */
class FunctionsAuditoriaRechazosRepositoryTest {

    private lateinit var functions: FirebaseFunctions
    private lateinit var callable: HttpsCallableReference
    private lateinit var repo: FunctionsAuditoriaRechazosRepository

    @Before
    fun setUp() {
        functions = mockk()
        callable = mockk()
        every { functions.getHttpsCallable("registrarRechazo") } returns callable
        repo = FunctionsAuditoriaRechazosRepository(functions)
    }

    @Test
    fun `envia coleccion, documento, operacion y origen android (sin autor)`() = runTest {
        val payload = slot<Any>()
        every { callable.call(capture(payload)) } returns Tasks.forResult(mockk<HttpsCallableResult>())

        val ok = repo.registrarRechazo("gastos", "doc-1", OperacionRechazada.CREATE)

        assertTrue(ok)
        assertEquals(
            mapOf(
                "coleccion" to "gastos",
                "documentoId" to "doc-1",
                "operacion" to "create",
                "origen" to "android"
            ),
            payload.captured
        )
    }

    @Test
    fun `es de mejor esfuerzo - un fallo devuelve false sin lanzar`() = runTest {
        every { callable.call(any()) } returns Tasks.forException(RuntimeException("sin red"))

        assertFalse(repo.registrarRechazo("gastos", "doc-1", OperacionRechazada.UPDATE))
    }
}
