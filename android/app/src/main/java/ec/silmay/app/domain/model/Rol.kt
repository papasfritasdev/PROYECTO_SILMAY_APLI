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

    /** RBAC móvil (HU-01a CA4): solo los roles de campo operan la app Android. */
    val habilitadoEnMovil: Boolean
        get() = this == OPERADOR || this == SUPERVISOR

    companion object {
        fun desdeValor(valor: String?): Rol? =
            entries.find { it.valor.equals(valor?.trim(), ignoreCase = true) }
    }
}
