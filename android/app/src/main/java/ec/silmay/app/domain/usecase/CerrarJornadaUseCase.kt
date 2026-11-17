package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.CalculadoraJornada
import ec.silmay.app.domain.model.Deduccion
import ec.silmay.app.domain.model.EstadoJornada
import ec.silmay.app.domain.model.Jornada
import ec.silmay.app.domain.repository.JornadaRepository

/**
 * Parámetros para el cierre de una jornada de trabajo (HU-04 CA2, CA3).
 */
data class CerrarJornadaParams(
    val jornadaUuid: String,
    val horaSalidaMillis: Long,
    val horometroFinal: Double,
    val observaciones: String? = null,
    val deducciones: List<Deduccion> = emptyList()
)

/**
 * Caso de Uso: Cierre de jornada y cálculo automático de métricas (HU-04 CA2, CA3, RF-09, RF-10).
 *
 * Validaciones y cálculos:
 * - horometroFinal > horometroInicial (MSG-05).
 * - horaSalida > horaInicio (MSG-11).
 * - sum(horasRestadas) < horasBrutas (MSG-12).
 * - descripcion de falla >= 10 caracteres (MSG-06).
 * - Sellado a estado 'cerrada' e inmutabilidad local en disco.
 *
 * Puro Kotlin (Clean Architecture estricta).
 */
class CerrarJornadaUseCase(
    private val jornadaRepository: JornadaRepository
) {
    suspend operator fun invoke(params: CerrarJornadaParams): AppResult<Jornada> {
        val consulta = jornadaRepository.obtenerJornadaPorUuid(params.jornadaUuid)
        if (consulta !is AppResult.Success || consulta.data == null) {
            return AppResult.Error(AppMessage.MSG_18)
        }

        val jornadaActual = consulta.data
        if (jornadaActual.estado == EstadoJornada.CERRADA) {
            return AppResult.Error(AppMessage.MSG_18)
        }

        val calculoResult = CalculadoraJornada.calcularCierre(
            horaInicioMillis = jornadaActual.horaInicioMillis,
            horaSalidaMillis = params.horaSalidaMillis,
            horometroInicial = jornadaActual.horometroInicial,
            horometroFinal = params.horometroFinal,
            deducciones = params.deducciones
        )

        val calculo = when (calculoResult) {
            is AppResult.Success -> calculoResult.data
            is AppResult.Error -> return calculoResult
        }

        val jornadaCerrada = jornadaActual.copy(
            horaSalidaMillis = params.horaSalidaMillis,
            horometroFinal = params.horometroFinal,
            horasBrutas = calculo.horasBrutas,
            horasRestadas = calculo.horasRestadas,
            horasEfectivas = calculo.horasEfectivas,
            deltaHorometro = calculo.deltaHorometro,
            observaciones = params.observaciones?.trim()?.ifBlank { null },
            deducciones = params.deducciones,
            tieneFallaCritica = calculo.tieneFallaCritica,
            estado = EstadoJornada.CERRADA
        )

        return when (val guardado = jornadaRepository.guardarJornada(jornadaCerrada)) {
            is AppResult.Success -> AppResult.Success(jornadaCerrada)
            is AppResult.Error -> AppResult.Error(guardado.message, guardado.throwable)
        }
    }
}
