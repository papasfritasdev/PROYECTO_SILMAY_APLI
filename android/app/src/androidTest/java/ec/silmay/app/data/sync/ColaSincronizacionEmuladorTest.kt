package ec.silmay.app.data.sync

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import ec.silmay.app.data.device.DispositivoRepositoryImpl
import ec.silmay.app.data.firestore.FirestoreProvider
import ec.silmay.app.data.repository.AuthRepositoryImpl
import ec.silmay.app.data.repository.FirestoreRegistroOperativoRepository
import ec.silmay.app.data.repository.FunctionsAuditoriaRechazosRepository
import ec.silmay.app.di.AppModule
import ec.silmay.app.di.SyncModule
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ColeccionOperativa
import ec.silmay.app.domain.usecase.GuardarRegistroOperativoUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.UUID
import kotlin.system.measureTimeMillis

/**
 * HT-07 contra Firebase Emulator Suite (Auth 9099, Firestore 8080, Functions 5001) con las
 * Security Rules reales. Requiere `firebase emulators:start` desde la raíz del repo y un
 * Android Emulator (10.0.2.2 = host).
 *
 * El cierre forzado se simula con `FirebaseFirestore.terminate()` + una instancia nueva:
 * la cola de mutaciones se recarga desde el mismo SQLite en disco, como tras reiniciar el
 * proceso o el dispositivo.
 */
@RunWith(AndroidJUnit4::class)
class ColaSincronizacionEmuladorTest {

    private companion object {
        const val HOST = "http://10.0.2.2"
        const val PROYECTO = "silmay-dev"
        const val ZONA = "cantera-banos"
        const val TOTAL_MUTACIONES = 50
        const val LIMITE_RNF01_MS = 2000L
        const val ESPERA_SYNC_MS = 120_000L
    }

    private val contexto: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val auth: FirebaseAuth = AppModule.provideFirebaseAuth()
    private val functions: FirebaseFunctions = SyncModule.provideFirebaseFunctions()
    private val prefsSync = contexto.getSharedPreferences("silmay_sync_test", Context.MODE_PRIVATE)
    private val prefsDispositivo = contexto.getSharedPreferences("silmay_dispositivo_test", Context.MODE_PRIVATE)
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var uid: String

    // ---------------------------------------------------------------- preparación

    @Before
    fun prepararOperador() = runBlocking {
        prefsSync.edit().clear().commit()
        val email = "op-ht07-${UUID.randomUUID().toString().take(8)}@silmay.ec"
        val contrasena = "Silmay2026!Prueba"

        val alta = http(
            "$HOST:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=clave-emulador",
            JSONObject().put("email", email).put("password", contrasena).put("returnSecureToken", true)
        ) as JSONObject
        uid = alta.getString("localId")

        http(
            "$HOST:9099/identitytoolkit.googleapis.com/v1/projects/$PROYECTO/accounts:update",
            JSONObject().put("localId", uid).put("customAttributes", """{"role":"operador"}""")
        )
        http(
            "$HOST:8080/v1/projects/$PROYECTO/databases/(default)/documents/usuarios?documentId=$uid",
            JSONObject().put(
                "fields",
                JSONObject()
                    .put("email", texto(email))
                    .put("nombres", texto("Operador HT-07"))
                    .put("cedula", texto("0102030405"))
                    .put("rol", texto("operador"))
                    .put("zonaAsignada", texto(ZONA))
                    .put("estado", JSONObject().put("booleanValue", true))
            )
        )

        auth.signInWithEmailAndPassword(email, contrasena).await()
        auth.currentUser!!.getIdToken(true).await()

        // Como en el login real: el perfil queda en caché para operar sin red.
        firestoreActiva().collection("usuarios").document(uid).get(Source.SERVER).await()
        Unit
    }

    @After
    fun limpiar() = runBlocking {
        runCatching { firestoreActiva().enableNetwork().await() }
        auth.signOut()
        prefsSync.edit().clear().commit()
        Unit
    }

    // ---------------------------------------------------------------- pruebas

    @Test
    fun cincuentaMutacionesOfflineSobrevivenAlCierreYSeTransmitenEnOrdenFifo() = runBlocking {
        val firestore = firestoreActiva()
        firestore.disableNetwork().await()
        val guardar = casoDeUso(firestore)

        val ids = mutableListOf<String>()
        repeat(TOTAL_MUTACIONES) { i ->
            var resultado: AppResult<String>? = null
            val ms = measureTimeMillis {
                resultado = guardar(ColeccionOperativa.GASTOS, gastoValido(montoUSD = i + 1.0))
            }
            val uuid = (resultado as AppResult.Success).data
            assertTrue("RNF-01: guardado #$i tardó $ms ms", ms <= LIMITE_RNF01_MS)
            ids += uuid
        }
        assertEquals(TOTAL_MUTACIONES, BitacoraPendientesPreferencias(prefsSync).listar().size)

        // "Cierre forzado": se descarta la instancia sin haber transmitido nada.
        firestore.terminate().await()
        val reabierta = firestoreActiva()

        withTimeout(ESPERA_SYNC_MS) { reabierta.waitForPendingWrites().await() }

        val sellos = ids.mapIndexed { i, uuid ->
            val doc = reabierta.collection("gastos").document(uuid).get(Source.SERVER).await()
            assertTrue("gasto #$i ($uuid) no llegó al servidor", doc.exists())
            assertEquals(uuid, doc.getString("uuid"))
            assertEquals(uid, doc.getString("userId"))
            assertEquals(ZONA, doc.getString("zona"))
            assertTrue(!doc.getString("deviceId").isNullOrBlank())
            assertTrue(doc.get("createdAtClient") is Timestamp)
            assertEquals(i + 1.0, doc.getDouble("montoUSD")!!, 0.0)
            doc.getTimestamp("syncedAtServer")!!
        }
        // FIFO: el servidor recibió las mutaciones en el mismo orden en que se capturaron.
        sellos.zipWithNext().forEachIndexed { i, (anterior, siguiente) ->
            assertTrue("orden roto entre #$i y #${i + 1}", anterior <= siguiente)
        }

        // Todas confirmadas: la bitácora queda vacía y no hay auditorías.
        val conciliador = conciliador(reabierta)
        conciliador.conciliar()
        assertTrue(BitacoraPendientesPreferencias(prefsSync).listar().isEmpty())
        assertEquals(0, auditoriasDe(ids.first()))
    }

    @Test
    fun rechazoPermissionDeniedTrasReinicioSeAuditaConOrigenAndroid() = runBlocking {
        val firestore = firestoreActiva()
        firestore.disableNetwork().await()

        // Local: se guarda sin red. Servidor: las reglas lo rechazarán (rubro fuera del enum).
        val resultado = casoDeUso(firestore)(
            ColeccionOperativa.GASTOS,
            gastoValido(montoUSD = 10.0) + ("rubro" to "rubro-inexistente")
        )
        val uuid = (resultado as AppResult.Success).data

        // Se cierra la app antes de que el servidor responda: se pierde el callback del Task.
        firestore.terminate().await()
        val reabierta = firestoreActiva()

        val quedan = withTimeout(ESPERA_SYNC_MS) { conciliador(reabierta).conciliar() }

        assertEquals(false, quedan)
        assertTrue(BitacoraPendientesPreferencias(prefsSync).listar().isEmpty())

        val auditorias = consultarAuditoria(uuid)
        assertEquals(1, auditorias.length())
        val campos = auditorias.getJSONObject(0).getJSONObject("document").getJSONObject("fields")
        assertEquals("PERMISSION_DENIED", campos.getJSONObject("accion").getString("stringValue"))
        assertEquals("gastos", campos.getJSONObject("coleccion").getString("stringValue"))
        assertEquals(uid, campos.getJSONObject("usuarioId").getString("stringValue"))
        val detalle = campos.getJSONObject("detalle").getJSONObject("mapValue").getJSONObject("fields")
        assertEquals("android", detalle.getJSONObject("origen").getString("stringValue"))
        assertEquals("create", detalle.getJSONObject("operacion").getString("stringValue"))
    }

    // ---------------------------------------------------------------- utilidades

    /** Instancia por defecto; si es nueva (tras terminate) se configura con caché y emulador. */
    private fun firestoreActiva(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        return try {
            FirestoreProvider.configurar(firestore)
        } catch (_: IllegalStateException) {
            firestore // ya configurada y en uso
        }
    }

    private fun conciliador(firestore: FirebaseFirestore) = ConciliadorSincronizacion(
        firestore = firestore,
        auth = auth,
        bitacora = BitacoraPendientesPreferencias(prefsSync),
        auditoria = FunctionsAuditoriaRechazosRepository(functions),
        alcance = alcance
    )

    private fun casoDeUso(firestore: FirebaseFirestore) = GuardarRegistroOperativoUseCase(
        authRepository = AuthRepositoryImpl(auth, firestore),
        dispositivoRepository = DispositivoRepositoryImpl(prefsDispositivo),
        registroOperativoRepository = FirestoreRegistroOperativoRepository(
            firestore,
            BitacoraPendientesPreferencias(prefsSync),
            conciliador(firestore)
        ),
        generadorUuid = GeneradorUuidV4(),
        reloj = RelojSistema()
    )

    private fun gastoValido(montoUSD: Double): Map<String, Any?> = mapOf(
        "rubro" to "combustible",
        "descripcion" to null,
        "montoUSD" to montoUSD,
        "numeroComprobante" to null,
        "unidadId" to "EXC-01",
        "fechaHora" to Timestamp(Date()),
        "estadoValidacion" to "pendiente",
        "validadoPor" to null,
        "fechaValidacion" to null,
        "comentarioValidacion" to null
    )

    private suspend fun auditoriasDe(documentoId: String): Int = consultarAuditoria(documentoId).length()

    private suspend fun consultarAuditoria(documentoId: String): JSONArray {
        val consulta = JSONObject().put(
            "structuredQuery",
            JSONObject()
                .put("from", JSONArray().put(JSONObject().put("collectionId", "auditoria")))
                .put(
                    "where",
                    JSONObject().put(
                        "fieldFilter",
                        JSONObject()
                            .put("field", JSONObject().put("fieldPath", "documentoId"))
                            .put("op", "EQUAL")
                            .put("value", texto(documentoId))
                    )
                )
        )
        val filas = http(
            "$HOST:8080/v1/projects/$PROYECTO/databases/(default)/documents:runQuery",
            consulta
        ) as JSONArray
        val documentos = JSONArray()
        for (i in 0 until filas.length()) {
            val fila = filas.getJSONObject(i)
            if (fila.has("document")) documentos.put(fila)
        }
        return documentos
    }

    private fun texto(valor: String) = JSONObject().put("stringValue", valor)

    /** POST JSON con privilegios de propietario del emulador (omite las Security Rules). */
    private suspend fun http(url: String, cuerpo: JSONObject): Any = withContext(Dispatchers.IO) {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "POST"
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json")
            conexion.setRequestProperty("Authorization", "Bearer owner")
            conexion.outputStream.use { it.write(cuerpo.toString().toByteArray()) }
            val codigo = conexion.responseCode
            val respuesta = (if (codigo in 200..299) conexion.inputStream else conexion.errorStream)
                .bufferedReader().use { it.readText() }
            check(codigo in 200..299) { "HTTP $codigo en $url: $respuesta" }
            if (respuesta.trimStart().startsWith("[")) JSONArray(respuesta) else JSONObject(respuesta)
        } finally {
            conexion.disconnect()
        }
    }
}
