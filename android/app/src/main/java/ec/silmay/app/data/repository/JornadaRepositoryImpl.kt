package ec.silmay.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ColeccionOperativa
import ec.silmay.app.domain.model.Coordenadas
import ec.silmay.app.domain.model.Deduccion
import ec.silmay.app.domain.model.EstadoJornada
import ec.silmay.app.domain.model.Jornada
import ec.silmay.app.domain.model.MetadatosControl
import ec.silmay.app.domain.model.SeveridadFalla
import ec.silmay.app.domain.repository.JornadaRepository
import ec.silmay.app.domain.repository.RegistroOperativoRepository
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación de JornadaRepository para persistencia y consulta offline-first (HU-04).
 * Utiliza [RegistroOperativoRepository] para garantizar escrituras locales atómicas con UUID v4,
 * y lee de la caché de Firestore (Source.CACHE) para disponibilidad plena en modo avión (RNF-12).
 */
class JornadaRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val registroOperativoRepository: RegistroOperativoRepository
) : JornadaRepository {

    override suspend fun guardarJornada(jornada: Jornada): AppResult<String> {
        val metadatos = MetadatosControl(
            uuid = jornada.uuid,
            createdAtClientMillis = jornada.createdAtClientMillis,
            userId = jornada.userId,
            deviceId = jornada.deviceId,
            zona = jornada.zona
        )

        val datos = mutableMapOf<String, Any?>(
            "maquinaId" to jornada.maquinaId,
            "lugarId" to jornada.lugarId,
            "horaInicio" to Timestamp(Date(jornada.horaInicioMillis)),
            "horaSalida" to jornada.horaSalidaMillis?.let { Timestamp(Date(it)) },
            "horometroInicial" to jornada.horometroInicial,
            "horometroFinal" to jornada.horometroFinal,
            "horasBrutas" to jornada.horasBrutas,
            "horasRestadas" to jornada.horasRestadas,
            "horasEfectivas" to jornada.horasEfectivas,
            "deltaHorometro" to jornada.deltaHorometro,
            "observaciones" to jornada.observaciones,
            "estado" to jornada.estado.valor,
            "tieneFallaCritica" to jornada.tieneFallaCritica,
            "notificadaFalla" to jornada.notificadaFalla,
            "procesada" to jornada.procesada,
            "deducciones" to jornada.deducciones.map { d ->
                mapOf(
                    "id" to d.id,
                    "motivo" to d.motivo,
                    "duracionHoras" to d.duracionHoras,
                    "esFalla" to d.esFalla,
                    "severidad" to d.severidad?.valor,
                    "descripcion" to d.descripcion,
                    "coordenadas" to d.coordenadas?.let { mapOf("lat" to it.lat, "lng" to it.lng) }
                )
            }
        )

        return registroOperativoRepository.guardar(
            ColeccionOperativa.JORNADAS,
            metadatos,
            datos
        )
    }

    override suspend fun obtenerJornadaAbierta(userId: String): AppResult<Jornada?> = try {
        val query = firestore.collection(ColeccionOperativa.JORNADAS.valor)
            .whereEqualTo("userId", userId)
            .whereEqualTo("estado", EstadoJornada.ABIERTA.valor)

        val snapshot = try {
            query.get(Source.CACHE).await()
        } catch (_: Exception) {
            query.get(Source.DEFAULT).await()
        }

        val primerDoc = snapshot.documents.firstOrNull()
        if (primerDoc != null && primerDoc.exists()) {
            AppResult.Success(mapearJornada(primerDoc))
        } else {
            AppResult.Success(null)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Error(AppMessage.MSG_18, e)
    }

    override suspend fun obtenerJornadaPorUuid(uuid: String): AppResult<Jornada?> = try {
        val docRef = firestore.collection(ColeccionOperativa.JORNADAS.valor).document(uuid)
        val doc = try {
            docRef.get(Source.CACHE).await()
        } catch (_: Exception) {
            docRef.get(Source.DEFAULT).await()
        }

        if (doc.exists()) {
            AppResult.Success(mapearJornada(doc))
        } else {
            AppResult.Success(null)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Error(AppMessage.MSG_18, e)
    }

    override suspend fun obtenerUltimoHorometroMaquina(maquinaId: String): AppResult<Double?> = try {
        val docRef = firestore.collection("maquinaria").document(maquinaId)
        val doc = try {
            docRef.get(Source.CACHE).await()
        } catch (_: Exception) {
            docRef.get(Source.DEFAULT).await()
        }

        if (doc.exists()) {
            val horometroActual = doc.getDouble("horometroActual")
                ?: doc.getDouble("horometroBase")
            AppResult.Success(horometroActual)
        } else {
            AppResult.Success(null)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Error(AppMessage.MSG_18, e)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun mapearJornada(doc: DocumentSnapshot): Jornada {
            val uuid = doc.getString("uuid") ?: doc.id
            val maquinaId = doc.getString("maquinaId") ?: ""
            val lugarId = doc.getString("lugarId") ?: ""

            val horaInicioMillis = doc.getTimestamp("horaInicio")?.toDate()?.time
                ?: (doc.getLong("horaInicio") ?: 0L)
            val horaSalidaMillis = doc.getTimestamp("horaSalida")?.toDate()?.time
                ?: doc.getLong("horaSalida")

            val horometroInicial = doc.getDouble("horometroInicial") ?: 0.0
            val horometroFinal = doc.getDouble("horometroFinal")
            val horasBrutas = doc.getDouble("horasBrutas")
            val horasRestadas = doc.getDouble("horasRestadas") ?: 0.0
            val horasEfectivas = doc.getDouble("horasEfectivas")
            val deltaHorometro = doc.getDouble("deltaHorometro")
            val observaciones = doc.getString("observaciones")

            val estado = EstadoJornada.desdeValor(doc.getString("estado"))
            val tieneFallaCritica = doc.getBoolean("tieneFallaCritica") ?: false
            val notificadaFalla = doc.getBoolean("notificadaFalla") ?: false
            val procesada = doc.getBoolean("procesada") ?: false

            @Suppress("UNCHECKED_CAST")
            val deduccionesRaw = doc.get("deducciones") as? List<Map<String, Any?>>
            val deducciones = deduccionesRaw?.mapNotNull { map ->
                val id = map["id"] as? String ?: return@mapNotNull null
                val motivo = map["motivo"] as? String ?: return@mapNotNull null
                val duracionHoras = (map["duracionHoras"] as? Number)?.toDouble() ?: 0.0
                val esFalla = map["esFalla"] as? Boolean ?: false
                val severidad = SeveridadFalla.desdeValor(map["severidad"] as? String)
                val descripcion = map["descripcion"] as? String
                val coordsMap = map["coordenadas"] as? Map<String, Any?>
                val coordenadas = if (coordsMap != null) {
                    val lat = (coordsMap["lat"] as? Number)?.toDouble() ?: 0.0
                    val lng = (coordsMap["lng"] as? Number)?.toDouble() ?: 0.0
                    Coordenadas(lat, lng)
                } else null

                Deduccion(
                    id = id,
                    motivo = motivo,
                    duracionHoras = duracionHoras,
                    esFalla = esFalla,
                    severidad = severidad,
                    descripcion = descripcion,
                    coordenadas = coordenadas
                )
            } ?: emptyList()

            val userId = doc.getString("userId") ?: ""
            val deviceId = doc.getString("deviceId") ?: ""
            val zona = doc.getString("zona") ?: ""
            val createdAtClientMillis = doc.getTimestamp("createdAtClient")?.toDate()?.time
                ?: (doc.getLong("createdAtClient") ?: System.currentTimeMillis())

            return Jornada(
                uuid = uuid,
                maquinaId = maquinaId,
                lugarId = lugarId,
                horaInicioMillis = horaInicioMillis,
                horaSalidaMillis = horaSalidaMillis,
                horometroInicial = horometroInicial,
                horometroFinal = horometroFinal,
                horasBrutas = horasBrutas,
                horasRestadas = horasRestadas,
                horasEfectivas = horasEfectivas,
                deltaHorometro = deltaHorometro,
                observaciones = observaciones,
                estado = estado,
                tieneFallaCritica = tieneFallaCritica,
                notificadaFalla = notificadaFalla,
                procesada = procesada,
                deducciones = deducciones,
                userId = userId,
                deviceId = deviceId,
                zona = zona,
                createdAtClientMillis = createdAtClientMillis
            )
        }
    }
}
