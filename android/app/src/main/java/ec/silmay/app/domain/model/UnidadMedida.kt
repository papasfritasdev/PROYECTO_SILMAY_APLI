package ec.silmay.app.domain.model

/**
 * Unidad de medida para despacho de material pétreo (docs/firebase-schema.md §2.3).
 * Puro Kotlin (Clean Architecture).
 */
enum class UnidadMedida(val valor: String) {
    M3("m3"),
    VIAJES("viajes");

    companion object {
        fun desdeValor(valor: String?): UnidadMedida =
            entries.find { it.valor.equals(valor, ignoreCase = true) } ?: M3
    }
}
