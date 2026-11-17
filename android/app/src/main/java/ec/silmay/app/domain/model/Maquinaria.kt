package ec.silmay.app.domain.model

/**
 * Inventario unificado de equipos pesados y volquetas (docs/firebase-schema.md §2.5).
 * Colección: maquinaria
 * Puro Kotlin (Clean Architecture).
 */
data class Maquinaria(
    val codigoInterno: String,
    val placa: String? = null,
    val marca: String,
    val modelo: String,
    val tipoEquipo: String,
    val horometroBase: Double = 0.0,
    val horometroActual: Double = 0.0,
    val intervaloMantenimiento: Double = 250.0,
    val proximoMantenimientoHoras: Double = 250.0,
    val estado: String = "operativa"
) {
    val estaOperativa: Boolean get() = estado.equals("operativa", ignoreCase = true)
    val esVolqueta: Boolean get() = tipoEquipo.equals("volqueta", ignoreCase = true)
}
