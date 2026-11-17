package ec.silmay.app.domain.model

/**
 * Severidad de avería o falla mecánica según docs/firebase-schema.md §2.2.
 * Puro Kotlin (Clean Architecture).
 */
enum class SeveridadFalla(val valor: String) {
    LEVE("leve"),
    MODERADA("moderada"),
    CRITICA("critica");

    companion object {
        fun desdeValor(valor: String?): SeveridadFalla? =
            entries.find { it.valor.equals(valor, ignoreCase = true) }
    }
}
