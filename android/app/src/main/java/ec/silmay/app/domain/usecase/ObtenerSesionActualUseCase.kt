package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.AuthRepository

/**
 * Caso de Uso: Recuperar la sesión persistida en disco (HU-01a CA1, sync-protocol §5).
 * Permite reabrir la app sin conexión tras el primer login. Reaplica las mismas reglas
 * de negocio del inicio de sesión: una sesión de cuenta deshabilitada o de un rol no
 * habilitado en móvil se revoca y se descarta.
 *
 * Puro Kotlin (Clean Architecture estricta).
 */
class ObtenerSesionActualUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): UsuarioSesion? {
        val sesion = authRepository.obtenerSesionActual() ?: return null

        if (!sesion.estado || !sesion.rol.habilitadoEnMovil) {
            authRepository.cerrarSesion()
            return null
        }
        return sesion
    }
}
