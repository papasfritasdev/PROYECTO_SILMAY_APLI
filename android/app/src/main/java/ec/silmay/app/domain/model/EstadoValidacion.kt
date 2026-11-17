package ec.silmay.app.domain.model

/**
 * Estado de validación contable para despachos y gastos (docs/firebase-schema.md §2.3, §2.4).
 * Puro Kotlin (Clean Architecture).
 */
enum class EstadoValidacion(val valor: String) {
    PENDIENTE("pendiente"),
    VALIDADO("validado"),
    OBSERVADO("observado");

    companion object {
        fun desdeValor(valor: String?): EstadoValidacion =
            entries.find { it.valor.equals(valor, ignoreCase = true) } ?: PENDIENTE
    }
}
