package ec.silmay.app.domain.model

/**
 * Entidad de dominio que representa una venta y despacho de pétreos (docs/firebase-schema.md §2.3).
 * Colección: despachos
 * Puro Kotlin (Clean Architecture).
 */
data class Despacho(
    val uuid: String,
    val materialId: String,
    val unidadMedida: UnidadMedida,
    val cantidad: Double,
    val clienteId: String,
    val volquetaId: String,
    val lugarId: String,
    val metodoCobro: MetodoCobro,
    val valorUSD: Double,
    val fechaHoraMillis: Long,
    val estadoValidacion: EstadoValidacion = EstadoValidacion.PENDIENTE,
    val validadoPor: String? = null,
    val fechaValidacionMillis: Long? = null,
    val comentarioValidacion: String? = null,
    // Metadatos de control obligatorios (docs/firebase-schema.md §1)
    val userId: String,
    val deviceId: String,
    val zona: String,
    val createdAtClientMillis: Long = System.currentTimeMillis()
)
