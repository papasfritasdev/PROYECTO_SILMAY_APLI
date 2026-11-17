package ec.silmay.app.domain.model

/**
 * Tipos autorizados de catálogos maestros según docs/firebase-schema.md §2.10.
 * Puro Kotlin (Clean Architecture).
 */
enum class TipoCatalogo(val valor: String) {
    MATERIALES("materiales"),
    CLIENTES("clientes"),
    LUGARES("lugares"),
    RUBROS("rubros"),
    MOTIVOS("motivos");

    companion object {
        fun desdeValor(valor: String): TipoCatalogo? =
            entries.find { it.valor.equals(valor, ignoreCase = true) }
    }
}
