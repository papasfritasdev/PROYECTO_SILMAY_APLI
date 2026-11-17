package ec.silmay.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ColeccionOperativa
import ec.silmay.app.domain.model.Despacho
import ec.silmay.app.domain.model.EstadoValidacion
import ec.silmay.app.domain.model.MetadatosControl
import ec.silmay.app.domain.model.MetodoCobro
import ec.silmay.app.domain.model.UnidadMedida
import ec.silmay.app.domain.repository.DespachoRepository
import ec.silmay.app.domain.repository.RegistroOperativoRepository
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación de DespachoRepository con estrategia Offline-First (HU-03).
 * Persiste localmente mediante [RegistroOperativoRepository] en $\le 2.0$ s (RNF-01).
 * Lee de la caché de Firestore (Source.CACHE) para permitir listar registros en modo avión.
 */
class DespachoRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val registroOperativoRepository: RegistroOperativoRepository
) : DespachoRepository {

    override suspend fun guardarDespacho(despacho: Despacho): AppResult<String> {
        val metadatos = MetadatosControl(
            uuid = despacho.uuid,
            createdAtClientMillis = despacho.createdAtClientMillis,
            userId = despacho.userId,
            deviceId = despacho.deviceId,
            zona = despacho.zona
        )

        val datos = mapOf(
            "materialId" to despacho.materialId,
            "unidadMedida" to despacho.unidadMedida.valor,
            "cantidad" to despacho.cantidad,
            "clienteId" to despacho.clienteId,
            "volquetaId" to despacho.volquetaId,
            "lugarId" to despacho.lugarId,
            "metodoCobro" to despacho.metodoCobro.valor,
            "valorUSD" to despacho.valorUSD,
            "fechaHora" to Timestamp(Date(despacho.fechaHoraMillis)),
            "estadoValidacion" to despacho.estadoValidacion.valor,
            "validadoPor" to despacho.validadoPor,
            "fechaValidacion" to despacho.fechaValidacionMillis?.let { Timestamp(Date(it)) },
            "comentarioValidacion" to despacho.comentarioValidacion
        )

        return registroOperativoRepository.guardar(
            ColeccionOperativa.DESPACHOS,
            metadatos,
            datos
        )
    }

    override suspend fun listarDespachosPorUsuario(userId: String): AppResult<List<Despacho>> = try {
        val query = firestore.collection(ColeccionOperativa.DESPACHOS.valor)
            .whereEqualTo("userId", userId)

        val snapshot = try {
            query.get(Source.CACHE).await()
        } catch (_: Exception) {
            query.get(Source.DEFAULT).await()
        }

        val lista = snapshot.documents.mapNotNull { doc ->
            if (doc.exists()) mapearDespacho(doc) else null
        }
        AppResult.Success(lista)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Error(AppMessage.MSG_18, e)
    }

    override suspend fun obtenerDespachoPorUuid(uuid: String): AppResult<Despacho?> = try {
        val docRef = firestore.collection(ColeccionOperativa.DESPACHOS.valor).document(uuid)
        val doc = try {
            docRef.get(Source.CACHE).await()
        } catch (_: Exception) {
            docRef.get(Source.DEFAULT).await()
        }

        if (doc.exists()) {
            AppResult.Success(mapearDespacho(doc))
        } else {
            AppResult.Success(null)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Error(AppMessage.MSG_18, e)
    }

    companion object {
        fun mapearDespacho(doc: DocumentSnapshot): Despacho {
            val uuid = doc.getString("uuid") ?: doc.id
            val materialId = doc.getString("materialId") ?: ""
            val unidadMedida = UnidadMedida.desdeValor(doc.getString("unidadMedida"))
            val cantidad = doc.getDouble("cantidad") ?: 0.0
            val clienteId = doc.getString("clienteId") ?: ""
            val volquetaId = doc.getString("volquetaId") ?: ""
            val lugarId = doc.getString("lugarId") ?: ""
            val metodoCobro = MetodoCobro.desdeValor(doc.getString("metodoCobro"))
            val valorUSD = doc.getDouble("valorUSD") ?: 0.0

            val fechaHoraMillis = doc.getTimestamp("fechaHora")?.toDate()?.time
                ?: (doc.getLong("fechaHora") ?: 0L)

            val estadoValidacion = EstadoValidacion.desdeValor(doc.getString("estadoValidacion"))
            val validadoPor = doc.getString("validadoPor")
            val fechaValidacionMillis = doc.getTimestamp("fechaValidacion")?.toDate()?.time
                ?: doc.getLong("fechaValidacion")
            val comentarioValidacion = doc.getString("comentarioValidacion")

            val userId = doc.getString("userId") ?: ""
            val deviceId = doc.getString("deviceId") ?: ""
            val zona = doc.getString("zona") ?: ""
            val createdAtClientMillis = doc.getTimestamp("createdAtClient")?.toDate()?.time
                ?: (doc.getLong("createdAtClient") ?: System.currentTimeMillis())

            return Despacho(
                uuid = uuid,
                materialId = materialId,
                unidadMedida = unidadMedida,
                cantidad = cantidad,
                clienteId = clienteId,
                volquetaId = volquetaId,
                lugarId = lugarId,
                metodoCobro = metodoCobro,
                valorUSD = valorUSD,
                fechaHoraMillis = fechaHoraMillis,
                estadoValidacion = estadoValidacion,
                validadoPor = validadoPor,
                fechaValidacionMillis = fechaValidacionMillis,
                comentarioValidacion = comentarioValidacion,
                userId = userId,
                deviceId = deviceId,
                zona = zona,
                createdAtClientMillis = createdAtClientMillis
            )
        }
    }
}
