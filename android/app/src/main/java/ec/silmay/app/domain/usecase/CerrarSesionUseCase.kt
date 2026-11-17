package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.SincronizacionRepository

/**
 * Resultado estructurado del intento de cierre de sesión (HU-07 CA3 / RF-04).
 */
sealed interface CerrarSesionResultado {
    data object Exito : CerrarSesionResultado
    data class BloqueadoPorPendientes(val cantidadPendientes: Int) : CerrarSesionResultado
    data class Error(val message: AppMessage, val throwable: Throwable? = null) : CerrarSesionResultado
}

/**
 * Caso de Uso: Cierre de sesión en la app móvil con protección de datos offline (HU-07 CA3 / RF-04).
 *
 * Si el usuario posee registros operativos locales pendientes de sincronizar en la cola del dispositivo,
 * el sistema advierte al usuario, bloquea el cierre de sesión y preserva intactos los datos en disco.
 *
 * Puro Kotlin (Clean Architecture estricta).
 */
class CerrarSesionUseCase(
    private val authRepository: AuthRepository,
    private val sincronizacionRepository: SincronizacionRepository
) {
    suspend operator fun invoke(): CerrarSesionResultado {
        val sesion = authRepository.obtenerSesionActual()
        if (sesion != null) {
            val pendientes = sincronizacionRepository.contarPendientes(sesion.uid)
            if (pendientes > 0) {
                return CerrarSesionResultado.BloqueadoPorPendientes(pendientes)
            }
        }

        return when (val resultado = authRepository.cerrarSesion()) {
            is AppResult.Success -> CerrarSesionResultado.Exito
            is AppResult.Error -> CerrarSesionResultado.Error(resultado.message, resultado.throwable)
        }
    }
}
