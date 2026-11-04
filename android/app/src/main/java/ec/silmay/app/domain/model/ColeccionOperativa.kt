package ec.silmay.app.domain.model

/**
 * Colecciones de registros operativos generados en el móvil (docs/firebase-schema.md §1).
 * Solo estas usan UUID v4 de cliente como Document ID y llevan los 6 campos de control.
 * Puro Kotlin.
 */
enum class ColeccionOperativa(val valor: String) {
    JORNADAS("jornadas"),
    DESPACHOS("despachos"),
    GASTOS("gastos");

    companion object {
        fun desdeValor(valor: String?): ColeccionOperativa? =
            entries.find { it.valor == valor }
    }
}
