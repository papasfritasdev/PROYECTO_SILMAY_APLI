package ec.silmay.app.domain.model

/**
 * Coordenadas geográficas capturadas puntualmente (docs/firebase-schema.md §2.2).
 */
data class Coordenadas(
    val lat: Double,
    val lng: Double
)

/**
 * Tiempo improductivo o novedad mecánica durante el turno de trabajo (docs/firebase-schema.md §2.2).
 * Puro Kotlin (Clean Architecture).
 */
data class Deduccion(
    val id: String,
    val motivo: String,
    val duracionHoras: Double,
    val esFalla: Boolean = false,
    val severidad: SeveridadFalla? = null,
    val descripcion: String? = null,
    val coordenadas: Coordenadas? = null
)
