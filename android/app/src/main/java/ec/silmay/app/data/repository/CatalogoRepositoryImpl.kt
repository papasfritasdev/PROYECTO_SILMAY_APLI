package ec.silmay.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ItemCatalogo
import ec.silmay.app.domain.model.Maquinaria
import ec.silmay.app.domain.model.TipoCatalogo
import ec.silmay.app.domain.repository.CatalogoRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación de CatalogoRepository con estrategia Offline-First (HU-07 CA2, CA4).
 * Prioriza la lectura desde la caché de Firestore (Source.CACHE) para garantizar
 * disponibilidad instantánea en campo (< 2 s, RNF-12). Si la caché no está disponible,
 * recurre a Source.DEFAULT (que intentará la red si está conectado).
 */
class CatalogoRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CatalogoRepository {

    override suspend fun obtenerItems(
        tipo: TipoCatalogo,
        soloActivos: Boolean
    ): AppResult<List<ItemCatalogo>> = try {
        val coleccion = firestore.collection("catalogos").document(tipo.valor).collection("items")
        val query = if (soloActivos) coleccion.whereEqualTo("activo", true) else coleccion

        val snapshot = try {
            query.get(Source.CACHE).await()
        } catch (_: Exception) {
            query.get(Source.DEFAULT).await()
        }

        val items = snapshot.documents.mapNotNull { doc ->
            val nombre = doc.getString("nombre") ?: return@mapNotNull null
            val activo = doc.getBoolean("activo") ?: true
            ItemCatalogo(
                id = doc.id,
                nombre = nombre,
                activo = activo
            )
        }
        AppResult.Success(items)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Error(AppMessage.MSG_18, e)
    }

    override suspend fun obtenerMaquinaria(
        soloOperativas: Boolean
    ): AppResult<List<Maquinaria>> = try {
        val coleccion = firestore.collection("maquinaria")
        val query = if (soloOperativas) coleccion.whereEqualTo("estado", "operativa") else coleccion

        val snapshot = try {
            query.get(Source.CACHE).await()
        } catch (_: Exception) {
            query.get(Source.DEFAULT).await()
        }

        val lista = snapshot.documents.mapNotNull { doc ->
            val codigoInterno = doc.getString("codigoInterno") ?: doc.id
            val marca = doc.getString("marca") ?: ""
            val modelo = doc.getString("modelo") ?: ""
            val tipoEquipo = doc.getString("tipoEquipo") ?: ""
            val placa = doc.getString("placa")
            val horometroBase = doc.getDouble("horometroBase") ?: 0.0
            val horometroActual = doc.getDouble("horometroActual") ?: horometroBase
            val intervalo = doc.getDouble("intervaloMantenimiento") ?: 250.0
            val proximoMantenimiento = doc.getDouble("proximoMantenimientoHoras") ?: (horometroBase + intervalo)
            val estado = doc.getString("estado") ?: "operativa"

            Maquinaria(
                codigoInterno = codigoInterno,
                placa = placa,
                marca = marca,
                modelo = modelo,
                tipoEquipo = tipoEquipo,
                horometroBase = horometroBase,
                horometroActual = horometroActual,
                intervaloMantenimiento = intervalo,
                proximoMantenimientoHoras = proximoMantenimiento,
                estado = estado
            )
        }
        AppResult.Success(lista)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Error(AppMessage.MSG_18, e)
    }

    override suspend fun obtenerMaquinariaPorCodigo(codigoInterno: String): AppResult<Maquinaria?> = try {
        val docRef = firestore.collection("maquinaria").document(codigoInterno)
        val doc = try {
            docRef.get(Source.CACHE).await()
        } catch (_: Exception) {
            docRef.get(Source.DEFAULT).await()
        }

        if (!doc.exists()) {
            AppResult.Success(null)
        } else {
            val codigo = doc.getString("codigoInterno") ?: doc.id
            val marca = doc.getString("marca") ?: ""
            val modelo = doc.getString("modelo") ?: ""
            val tipoEquipo = doc.getString("tipoEquipo") ?: ""
            val placa = doc.getString("placa")
            val horometroBase = doc.getDouble("horometroBase") ?: 0.0
            val horometroActual = doc.getDouble("horometroActual") ?: horometroBase
            val intervalo = doc.getDouble("intervaloMantenimiento") ?: 250.0
            val proximoMantenimiento = doc.getDouble("proximoMantenimientoHoras") ?: (horometroBase + intervalo)
            val estado = doc.getString("estado") ?: "operativa"

            AppResult.Success(
                Maquinaria(
                    codigoInterno = codigo,
                    placa = placa,
                    marca = marca,
                    modelo = modelo,
                    tipoEquipo = tipoEquipo,
                    horometroBase = horometroBase,
                    horometroActual = horometroActual,
                    intervaloMantenimiento = intervalo,
                    proximoMantenimientoHoras = proximoMantenimiento,
                    estado = estado
                )
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Error(AppMessage.MSG_18, e)
    }
}
