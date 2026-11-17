package ec.silmay.app.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ItemCatalogo
import ec.silmay.app.domain.model.TipoCatalogo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CatalogoRepositoryImplTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var repo: CatalogoRepositoryImpl

    @Before
    fun setUp() {
        firestore = mockk()
        repo = CatalogoRepositoryImpl(firestore)
    }

    @Test
    fun `obtenerItems con soloActivos mapea y retorna lista de items correctamente`() = runTest {
        val rootCol: CollectionReference = mockk()
        val docTipo: DocumentReference = mockk()
        val subCol: CollectionReference = mockk()
        val query: Query = mockk()
        val snapshot: QuerySnapshot = mockk()
        val doc1: DocumentSnapshot = mockk()
        val doc2: DocumentSnapshot = mockk()

        every { firestore.collection("catalogos") } returns rootCol
        every { rootCol.document("materiales") } returns docTipo
        every { docTipo.collection("items") } returns subCol
        every { subCol.whereEqualTo("activo", true) } returns query
        every { query.get(Source.CACHE) } returns Tasks.forResult(snapshot)
        every { snapshot.documents } returns listOf(doc1, doc2)

        every { doc1.id } returns "arena-fina"
        every { doc1.getString("nombre") } returns "Arena Fina"
        every { doc1.getBoolean("activo") } returns true

        every { doc2.id } returns "ripio"
        every { doc2.getString("nombre") } returns "Ripio"
        every { doc2.getBoolean("activo") } returns true

        val resultado = repo.obtenerItems(TipoCatalogo.MATERIALES, soloActivos = true)

        assertTrue(resultado is AppResult.Success)
        val items = (resultado as AppResult.Success).data
        assertEquals(2, items.size)
        assertEquals(ItemCatalogo("arena-fina", "Arena Fina", true), items[0])
        assertEquals(ItemCatalogo("ripio", "Ripio", true), items[1])
    }

    @Test
    fun `obtenerMaquinaria con soloOperativas mapea y retorna lista de maquinaria`() = runTest {
        val colMaquinaria: CollectionReference = mockk()
        val query: Query = mockk()
        val snapshot: QuerySnapshot = mockk()
        val doc: DocumentSnapshot = mockk()

        every { firestore.collection("maquinaria") } returns colMaquinaria
        every { colMaquinaria.whereEqualTo("estado", "operativa") } returns query
        every { query.get(Source.CACHE) } returns Tasks.forResult(snapshot)
        every { snapshot.documents } returns listOf(doc)

        every { doc.id } returns "EX-01"
        every { doc.getString("codigoInterno") } returns "EX-01"
        every { doc.getString("marca") } returns "CAT"
        every { doc.getString("modelo") } returns "320D"
        every { doc.getString("tipoEquipo") } returns "excavadora"
        every { doc.getString("placa") } returns null
        every { doc.getDouble("horometroBase") } returns 1500.0
        every { doc.getDouble("horometroActual") } returns 1620.5
        every { doc.getDouble("intervaloMantenimiento") } returns 250.0
        every { doc.getDouble("proximoMantenimientoHoras") } returns 1750.0
        every { doc.getString("estado") } returns "operativa"

        val resultado = repo.obtenerMaquinaria(soloOperativas = true)

        assertTrue(resultado is AppResult.Success)
        val lista = (resultado as AppResult.Success).data
        assertEquals(1, lista.size)
        val maq = lista[0]
        assertEquals("EX-01", maq.codigoInterno)
        assertEquals("CAT", maq.marca)
        assertEquals(1620.5, maq.horometroActual, 0.01)
        assertTrue(maq.estaOperativa)
    }

    @Test
    fun `obtenerMaquinariaPorCodigo retorna maquinaria existente`() = runTest {
        val colMaquinaria: CollectionReference = mockk()
        val docRef: DocumentReference = mockk()
        val doc: DocumentSnapshot = mockk()

        every { firestore.collection("maquinaria") } returns colMaquinaria
        every { colMaquinaria.document("VOL-02") } returns docRef
        every { docRef.get(Source.CACHE) } returns Tasks.forResult(doc)
        every { doc.exists() } returns true

        every { doc.id } returns "VOL-02"
        every { doc.getString("codigoInterno") } returns "VOL-02"
        every { doc.getString("marca") } returns "Hino"
        every { doc.getString("modelo") } returns "500"
        every { doc.getString("tipoEquipo") } returns "volqueta"
        every { doc.getString("placa") } returns "ABC-1234"
        every { doc.getDouble("horometroBase") } returns 500.0
        every { doc.getDouble("horometroActual") } returns 550.0
        every { doc.getDouble("intervaloMantenimiento") } returns 500.0
        every { doc.getDouble("proximoMantenimientoHoras") } returns 1000.0
        every { doc.getString("estado") } returns "operativa"

        val resultado = repo.obtenerMaquinariaPorCodigo("VOL-02")

        assertTrue(resultado is AppResult.Success)
        val maq = (resultado as AppResult.Success).data
        assertEquals("VOL-02", maq?.codigoInterno)
        assertEquals("ABC-1234", maq?.placa)
        assertTrue(maq?.esVolqueta == true)
    }

    @Test
    fun `obtenerMaquinariaPorCodigo retorna null si el documento no existe`() = runTest {
        val colMaquinaria: CollectionReference = mockk()
        val docRef: DocumentReference = mockk()
        val doc: DocumentSnapshot = mockk()

        every { firestore.collection("maquinaria") } returns colMaquinaria
        every { colMaquinaria.document("INEXISTENTE") } returns docRef
        every { docRef.get(Source.CACHE) } returns Tasks.forResult(doc)
        every { doc.exists() } returns false

        val resultado = repo.obtenerMaquinariaPorCodigo("INEXISTENTE")

        assertTrue(resultado is AppResult.Success)
        assertNull((resultado as AppResult.Success).data)
    }

    @Test
    fun `cuando ocurre una excepcion inesperada retorna AppResult Error con MSG_18`() = runTest {
        val rootCol: CollectionReference = mockk()
        every { firestore.collection("catalogos") } returns rootCol
        every { rootCol.document(any()) } throws RuntimeException("Error simulado de base de datos")

        val resultado = repo.obtenerItems(TipoCatalogo.MATERIALES)

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_18, (resultado as AppResult.Error).message)
    }
}
