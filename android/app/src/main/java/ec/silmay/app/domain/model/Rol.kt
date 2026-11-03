package ec.silmay.app.domain.model

/**
 * Roles autoritativos del Sistema SILMAY según docs/firebase-schema.md.
 * Asignados como Custom Claims en Firebase Authentication.
 */
enum class Rol(val valor: String) {
    OPERADOR("operador"),
    SUPERVISOR("supervisor"),
    CONTADOR("contador"),
    ADMIN("admin");

    companion object {
        fun desdeValor(valor: String?): Rol? =
            entries.find { it.valor.equals(valor?.trim(), ignoreCase = true) }
    }
}
