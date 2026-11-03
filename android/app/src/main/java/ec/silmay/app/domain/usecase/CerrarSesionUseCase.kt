package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.repository.AuthRepository

/**
 * Caso de Uso: Cierre de sesión en la app móvil (M7).
 *
 * Pendiente (Sprint 3, HU-08 / sync-protocol §5): bloquear el cierre cuando existan
 * registros en disco pendientes de sincronizar. En el Sprint 2 aún no hay capturas
 * operativas, por lo que el cierre es directo.
 *
 * Puro Kotlin (Clean Architecture estricta).
 */
class CerrarSesionUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppResult<Unit> = authRepository.cerrarSesion()
}
