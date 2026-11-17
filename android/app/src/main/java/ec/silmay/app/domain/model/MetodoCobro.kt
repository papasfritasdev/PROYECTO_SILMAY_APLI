package ec.silmay.app.domain.model

/**
 * Método de cobro para despacho de material pétreo (docs/firebase-schema.md §2.3).
 * Puro Kotlin (Clean Architecture).
 */
enum class MetodoCobro(val valor: String) {
    EFECTIVO("efectivo"),
    TRANSFERENCIA("transferencia"),
    CREDITO("credito");

    companion object {
        fun desdeValor(valor: String?): MetodoCobro =
            entries.find { it.valor.equals(valor, ignoreCase = true) } ?: EFECTIVO
    }
}
