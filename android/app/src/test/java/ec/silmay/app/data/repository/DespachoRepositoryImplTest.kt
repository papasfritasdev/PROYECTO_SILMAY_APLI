package ec.silmay.app.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ColeccionOperativa
import ec.silmay.app.domain.model.Despacho
import ec.silmay.app.domain.model.EstadoValidacion
import ec.silmay.app.domain.model.MetadatosControl
import ec.silmay.app.domain.model.MetodoCobro
import ec.silmay.app.domain.model.UnidadMedida
import ec.silmay.app.domain.repository.RegistroOperativoRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class DespachoRepositoryImplTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var registroOperativoRepository: RegistroOperativoRepository
    private lateinit var repo: DespachoRepositoryImpl

    private val despachoPrueba = Despacho(
        uuid = "despacho-uuid-1",
        materialId = "arena-fina",
        unidadMedida = UnidadMedida.M3,
        cantidad = 8.5,
        clienteId = "constructora-1",
        volquetaId = "VOL-01",
        lugarId = "frente-norte",
        metodoCobro = MetodoCobro.EFECTIVO,
        valorUSD = 127.50,
        fechaHoraMillis = 1_700_000_000_000L,
        estadoValidacion = EstadoValidacion.PENDIENTE,
        userId = "usr-1",
        deviceId = "dev-1",
        zona = "cantera-banos",
        createdAtClientMillis = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        firestore = mockk()
        registroOperativoRepository = mockk()
        repo = DespachoRepositoryImpl(firestore, registroOperativoRepository)
    }

    @Test
    fun `guardarDespacho serializa metadatos y datos y delega en registroOperativoRepository`() = runTest {
        val slotColeccion = slot<ColeccionOperativa>()
        val slotMetadatos = slot<MetadatosControl>()
        val slotDatos = slot<Map<String, Any?>>()

        coEvery {
            registroOperativoRepository.guardar(
                capture(slotColeccion),
                capture(slotMetadatos),
                capture(slotDatos)
            )
        } returns AppResult.Success("despacho-uuid-1")

        val resultado = repo.guardarDespacho(despachoPrueba)

        assertTrue(resultado is AppResult.Success)
        assertEquals("despacho-uuid-1", (resultado as AppResult.Success).data)
        assertEquals(ColeccionOperativa.DESPACHOS, slotColeccion.captured)
        assertEquals("despacho-uuid-1", slotMetadatos.captured.uuid)
        assertEquals("usr-1", slotMetadatos.captured.userId)
        assertEquals("cantera-banos", slotMetadatos.captured.zona)

        val datos = slotDatos.captured
        assertEquals("arena-fina", datos["materialId"])
        assertEquals("m3", datos["unidadMedida"])
        assertEquals(8.5, datos["cantidad"])
        assertEquals("constructora-1", datos["clienteId"])
        assertEquals("VOL-01", datos["volquetaId"])
        assertEquals("frente-norte", datos["lugarId"])
        assertEquals("efectivo", datos["metodoCobro"])
        assertEquals(127.50, datos["valorUSD"])
        assertEquals("pendiente", datos["estadoValidacion"])
    }

    @Test
    fun `listarDespachosPorUsuario consulta y deserializa los despachos en cache`() = runTest {
        val colDespachos: CollectionReference = mockk()
        val queryUser: Query = mockk()
        val snapshot: QuerySnapshot = mockk()
        val doc: DocumentSnapshot = mockk()

        every { firestore.collection(ColeccionOperativa.DESPACHOS.valor) } returns colDespachos
        every { colDespachos.whereEqualTo("userId", "usr-1") } returns queryUser
        every { queryUser.get(Source.CACHE) } returns Tasks.forResult(snapshot)
        every { snapshot.documents } returns listOf(doc)
        every { doc.exists() } returns true

        every { doc.id } returns "despacho-uuid-1"
        every { doc.getString("uuid") } returns "despacho-uuid-1"
        every { doc.getString("materialId") } returns "arena-fina"
        every { doc.getString("unidadMedida") } returns "m3"
        every { doc.getDouble("cantidad") } returns 8.5
        every { doc.getString("clienteId") } returns "constructora-1"
        every { doc.getString("volquetaId") } returns "VOL-01"
        every { doc.getString("lugarId") } returns "frente-norte"
        every { doc.getString("metodoCobro") } returns "efectivo"
        every { doc.getDouble("valorUSD") } returns 127.50
        every { doc.getTimestamp("fechaHora") } returns Timestamp(Date(1_700_000_000_000L))
        every { doc.getString("estadoValidacion") } returns "pendiente"
        every { doc.getString("validadoPor") } returns null
        every { doc.getTimestamp("fechaValidacion") } returns null
        every { doc.getLong("fechaValidacion") } returns null
        every { doc.getString("comentarioValidacion") } returns null
        every { doc.getString("userId") } returns "usr-1"
        every { doc.getString("deviceId") } returns "dev-1"
        every { doc.getString("zona") } returns "cantera-banos"
        every { doc.getTimestamp("createdAtClient") } returns Timestamp(Date(1_700_000_000_000L))

        val resultado = repo.listarDespachosPorUsuario("usr-1")

        assertTrue(resultado is AppResult.Success)
        val lista = (resultado as AppResult.Success).data
        assertEquals(1, lista.size)
        val d = lista[0]
        assertEquals("despacho-uuid-1", d.uuid)
        assertEquals(8.5, d.cantidad, 0.01)
        assertEquals(EstadoValidacion.PENDIENTE, d.estadoValidacion)
    }

    @Test
    fun `obtenerDespachoPorUuid retorna despacho existente`() = runTest {
        val colDespachos: CollectionReference = mockk()
        val docRef: DocumentReference = mockk()
        val doc: DocumentSnapshot = mockk()

        every { firestore.collection(ColeccionOperativa.DESPACHOS.valor) } returns colDespachos
        every { colDespachos.document("despacho-uuid-1") } returns docRef
        every { docRef.get(Source.CACHE) } returns Tasks.forResult(doc)
        every { doc.exists() } returns true

        every { doc.id } returns "despacho-uuid-1"
        every { doc.getString("uuid") } returns "despacho-uuid-1"
        every { doc.getString("materialId") } returns "arena-fina"
        every { doc.getString("unidadMedida") } returns "m3"
        every { doc.getDouble("cantidad") } returns 8.5
        every { doc.getString("clienteId") } returns "constructora-1"
        every { doc.getString("volquetaId") } returns "VOL-01"
        every { doc.getString("lugarId") } returns "frente-norte"
        every { doc.getString("metodoCobro") } returns "efectivo"
        every { doc.getDouble("valorUSD") } returns 127.50
        every { doc.getTimestamp("fechaHora") } returns Timestamp(Date(1_700_000_000_000L))
        every { doc.getString("estadoValidacion") } returns "pendiente"
        every { doc.getString("validadoPor") } returns null
        every { doc.getTimestamp("fechaValidacion") } returns null
        every { doc.getLong("fechaValidacion") } returns null
        every { doc.getString("comentarioValidacion") } returns null
        every { doc.getString("userId") } returns "usr-1"
        every { doc.getString("deviceId") } returns "dev-1"
        every { doc.getString("zona") } returns "cantera-banos"
        every { doc.getTimestamp("createdAtClient") } returns Timestamp(Date(1_700_000_000_000L))

        val resultado = repo.obtenerDespachoPorUuid("despacho-uuid-1")

        assertTrue(resultado is AppResult.Success)
        val d = (resultado as AppResult.Success).data
        assertNotNull(d)
        assertEquals("despacho-uuid-1", d?.uuid)
    }

    @Test
    fun `obtenerDespachoPorUuid cuando no existe retorna null`() = runTest {
        val colDespachos: CollectionReference = mockk()
        val docRef: DocumentReference = mockk()
        val doc: DocumentSnapshot = mockk()

        every { firestore.collection(ColeccionOperativa.DESPACHOS.valor) } returns colDespachos
        every { colDespachos.document("inexistente") } returns docRef
        every { docRef.get(Source.CACHE) } returns Tasks.forResult(doc)
        every { doc.exists() } returns false

        val resultado = repo.obtenerDespachoPorUuid("inexistente")

        assertTrue(resultado is AppResult.Success)
        assertNull((resultado as AppResult.Success).data)
    }
}
