package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.AuthRepository

/**
 * Caso de Uso: Autenticación por Rol en Aplicación Móvil (HU-01a).
 * Implementa las reglas de negocio y criterios de aceptación:
 * - CA1: Validación de credenciales y retorno de sesión.
 * - CA2: Mensaje opaco MSG-01 ante credenciales inválidas.
 * - CA3: Bloqueo de cuenta deshabilitada (estado == false) con MSG-03.
 * - CA4: Restricción RBAC Móvil (solo 'operador' o 'supervisor') con MSG-13.
 *
 * Puro Kotlin (Clean Architecture estricta).
 */
class IniciarSesionUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, contrasena: String): AppResult<UsuarioSesion> {
        val emailLimpio = email.trim()
        val contrasenaLimpia = contrasena.trim()

        if (emailLimpio.isEmpty() || contrasenaLimpia.isEmpty()) {
            return AppResult.Error(AppMessage.MSG_01)
        }

        val resultado = authRepository.iniciarSesion(emailLimpio, contrasenaLimpia)
        if (resultado is AppResult.Success) {
            val sesion = resultado.data

            // CA3: Baja Lógica - cuenta deshabilitada
            if (!sesion.estado) {
                authRepository.cerrarSesion()
                return AppResult.Error(AppMessage.MSG_03)
            }

            // CA4: Restricción de plataforma por rol (RBAC Móvil)
            // Solo 'operador' y 'supervisor' están autorizados en móvil.
            if (!sesion.rol.habilitadoEnMovil) {
                authRepository.cerrarSesion()
                return AppResult.Error(AppMessage.MSG_13)
            }
        }

        return resultado
    }
}
