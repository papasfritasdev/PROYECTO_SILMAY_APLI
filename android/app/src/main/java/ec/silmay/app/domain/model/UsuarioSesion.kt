package ec.silmay.app.domain.model

/**
 * Entidad de dominio que representa la sesión activa de un usuario en el cliente móvil.
 * Puro Kotlin (Clean Architecture).
 */
data class UsuarioSesion(
    val uid: String,
    val email: String,
    val nombres: String,
    val cedula: String,
    val rol: Rol,
    val zonaAsignada: String,
    val estado: Boolean
)
