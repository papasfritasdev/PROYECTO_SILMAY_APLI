package ec.silmay.app.domain.model

/**
 * Encapsulador de resultados para operaciones de Dominio.
 * Garantiza tratamiento seguro de éxito y fallo con mensajes normalizados del catálogo.
 * Puro Kotlin.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Error(val message: AppMessage, val throwable: Throwable? = null) : AppResult<Nothing>
}
