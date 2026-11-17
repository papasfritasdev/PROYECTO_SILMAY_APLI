package ec.silmay.app.domain.model

/**
 * Estados del ciclo de vida de una jornada de trabajo (docs/firebase-schema.md §2.2).
 * Puro Kotlin (Clean Architecture).
 */
enum class EstadoJornada(val valor: String) {
    ABIERTA("abierta"),
    CERRADA("cerrada");

    companion object {
        fun desdeValor(valor: String?): EstadoJornada =
            entries.find { it.valor.equals(valor, ignoreCase = true) } ?: ABIERTA
    }
}
