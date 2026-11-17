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
import ec.silmay.app.domain.model.Deduccion
import ec.silmay.app.domain.model.EstadoJornada
import ec.silmay.app.domain.model.Jornada
import ec.silmay.app.domain.model.MetadatosControl
import ec.silmay.app.domain.model.SeveridadFalla
import ec.silmay.app.domain.repository.RegistroOperativoRepository
import io.mockk.coEvery
import io.mockk.coVerify
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

class JornadaRepositoryImplTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var registroOperativoRepository: RegistroOperativoRepository
    private lateinit var repo: JornadaRepositoryImpl

    private val jornadaPrueba = Jornada(
        uuid = "jornada-uuid-1",
        maquinaId = "EX-01",
        lugarId = "frente-1",
        horaInicioMillis = 1_700_000_000_000L,
        horaSalidaMillis = 1_700_028_800_000L,
        horometroInicial = 1000.0,
        horometroFinal = 1008.0,
        horasBrutas = 8.0,
        horasRestadas = 1.0,
        horasEfectivas = 7.0,
        deltaHorometro = 8.0,
        observaciones = "Turno regular",
        estado = EstadoJornada.CERRADA,
        tieneFallaCritica = false,
        notificadaFalla = false,
        procesada = false,
        deducciones = listOf(
            Deduccion(id = "d1", motivo = "almuerzo", duracionHoras = 1.0)
        ),
        userId = "usr-1",
        deviceId = "dev-1",
        zona = "cantera-banos",
        createdAtClientMillis = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        firestore = mockk()
        registroOperativoRepository = mockk()
        repo = JornadaRepositoryImpl(firestore, registroOperativoRepository)
    }

    @Test
    fun `guardarJornada serializa metadatos y datos y delega en registroOperativoRepository`() = runTest {
        val slotColeccion = slot<ColeccionOperativa>()
        val slotMetadatos = slot<MetadatosControl>()
        val slotDatos = slot<Map<String, Any?>>()

        coEvery {
            registroOperativoRepository.guardar(
                capture(slotColeccion),
                capture(slotMetadatos),
                capture(slotDatos)
            )
        } returns AppResult.Success("jornada-uuid-1")

        val resultado = repo.guardarJornada(jornadaPrueba)

        assertTrue(resultado is AppResult.Success)
        assertEquals("jornada-uuid-1", (resultado as AppResult.Success).data)
        assertEquals(ColeccionOperativa.JORNADAS, slotColeccion.captured)
        assertEquals("jornada-uuid-1", slotMetadatos.captured.uuid)
        assertEquals("usr-1", slotMetadatos.captured.userId)
        assertEquals("cantera-banos", slotMetadatos.captured.zona)

        val datos = slotDatos.captured
        assertEquals("EX-01", datos["maquinaId"])
        assertEquals("frente-1", datos["lugarId"])
        assertEquals(1000.0, datos["horometroInicial"])
        assertEquals(1008.0, datos["horometroFinal"])
        assertEquals("cerrada", datos["estado"])
    }

    @Test
    fun `obtenerJornadaAbierta consulta y deserializa la jornada en cache`() = runTest {
        val colJornadas: CollectionReference = mockk()
        val queryUser: Query = mockk()
        val queryEstado: Query = mockk()
        val snapshot: QuerySnapshot = mockk()
        val doc: DocumentSnapshot = mockk()

        every { firestore.collection(ColeccionOperativa.JORNADAS.valor) } returns colJornadas
        every { colJornadas.whereEqualTo("userId", "usr-1") } returns queryUser
        every { queryUser.whereEqualTo("estado", "abierta") } returns queryEstado
        every { queryEstado.get(Source.CACHE) } returns Tasks.forResult(snapshot)
        every { snapshot.documents } returns listOf(doc)
        every { doc.exists() } returns true

        every { doc.id } returns "jornada-abierta-uuid"
        every { doc.getString("uuid") } returns "jornada-abierta-uuid"
        every { doc.getString("maquinaId") } returns "EX-01"
        every { doc.getString("lugarId") } returns "frente-1"
        every { doc.getTimestamp("horaInicio") } returns Timestamp(Date(1_700_000_000_000L))
        every { doc.getTimestamp("horaSalida") } returns null
        every { doc.getLong("horaSalida") } returns null
        every { doc.getDouble("horometroInicial") } returns 1000.0
        every { doc.getDouble("horometroFinal") } returns null
        every { doc.getDouble("horasBrutas") } returns null
        every { doc.getDouble("horasRestadas") } returns 0.0
        every { doc.getDouble("horasEfectivas") } returns null
        every { doc.getDouble("deltaHorometro") } returns null
        every { doc.getString("observaciones") } returns null
        every { doc.getString("estado") } returns "abierta"
        every { doc.getBoolean("tieneFallaCritica") } returns false
        every { doc.getBoolean("notificadaFalla") } returns false
        every { doc.getBoolean("procesada") } returns false
        every { doc.get("deducciones") } returns emptyList<Map<String, Any?>>()
        every { doc.getString("userId") } returns "usr-1"
        every { doc.getString("deviceId") } returns "dev-1"
        every { doc.getString("zona") } returns "cantera-banos"
        every { doc.getTimestamp("createdAtClient") } returns Timestamp(Date(1_700_000_000_000L))

        val resultado = repo.obtenerJornadaAbierta("usr-1")

        assertTrue(resultado is AppResult.Success)
        val jornada = (resultado as AppResult.Success).data
        assertNotNull(jornada)
        assertEquals("jornada-abierta-uuid", jornada?.uuid)
        assertEquals(EstadoJornada.ABIERTA, jornada?.estado)
        assertTrue(jornada?.estaAbierta == true)
    }

    @Test
    fun `obtenerJornadaAbierta cuando no hay jornada abierta retorna null`() = runTest {
        val colJornadas: CollectionReference = mockk()
        val queryUser: Query = mockk()
        val queryEstado: Query = mockk()
        val snapshot: QuerySnapshot = mockk()

        every { firestore.collection(ColeccionOperativa.JORNADAS.valor) } returns colJornadas
        every { colJornadas.whereEqualTo("userId", "usr-1") } returns queryUser
        every { queryUser.whereEqualTo("estado", "abierta") } returns queryEstado
        every { queryEstado.get(Source.CACHE) } returns Tasks.forResult(snapshot)
        every { snapshot.documents } returns emptyList()

        val resultado = repo.obtenerJornadaAbierta("usr-1")

        assertTrue(resultado is AppResult.Success)
        assertNull((resultado as AppResult.Success).data)
    }

    @Test
    fun `obtenerUltimoHorometroMaquina retorna horometroActual`() = runTest {
        val colMaquinaria: CollectionReference = mockk()
        val docRef: DocumentReference = mockk()
        val doc: DocumentSnapshot = mockk()

        every { firestore.collection("maquinaria") } returns colMaquinaria
        every { colMaquinaria.document("EX-01") } returns docRef
        every { docRef.get(Source.CACHE) } returns Tasks.forResult(doc)
        every { doc.exists() } returns true
        every { doc.getDouble("horometroActual") } returns 1650.5

        val resultado = repo.obtenerUltimoHorometroMaquina("EX-01")

        assertTrue(resultado is AppResult.Success)
        assertEquals(1650.5, (resultado as AppResult.Success).data!!, 0.01)
    }
}
