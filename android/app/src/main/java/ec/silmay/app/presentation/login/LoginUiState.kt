package ec.silmay.app.presentation.login

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.UsuarioSesion

/**
 * Estado inmutable de la pantalla de inicio de sesión (M7 / HU-01a).
 * Representa la única fuente de verdad para la UI en Jetpack Compose.
 */
data class LoginUiState(
    val email: String = "",
    val contrasena: String = "",
    val isLoading: Boolean = false,
    val mensajeError: AppMessage? = null,
    val sesionActiva: UsuarioSesion? = null
) {
    val puedeIniciarSesion: Boolean
        get() = email.isNotBlank() && contrasena.isNotBlank() && !isLoading
}
