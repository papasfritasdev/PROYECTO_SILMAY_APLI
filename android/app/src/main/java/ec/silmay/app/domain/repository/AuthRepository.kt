package ec.silmay.app.domain.repository

import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.UsuarioSesion

/**
 * Contrato de Dominio para operaciones de Autenticación y Sesión.
 * Define la abstracción sin acoplarse a Firebase ni a Android SDK.
 */
interface AuthRepository {
    suspend fun iniciarSesion(email: String, contrasena: String): AppResult<UsuarioSesion>
    suspend fun cerrarSesion(): AppResult<Unit>
    suspend fun obtenerSesionActual(): UsuarioSesion?
}
