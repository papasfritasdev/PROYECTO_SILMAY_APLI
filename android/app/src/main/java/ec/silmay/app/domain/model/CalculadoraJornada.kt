package ec.silmay.app.domain.model

import kotlin.math.round

/**
 * Resultado del cálculo aritmético y validación de cierre de jornada (RF-09, docs/business-formulas.md §1).
 */
data class CalculoCierreJornada(
    val horasBrutas: Double,
    val horasRestadas: Double,
    val horasEfectivas: Double,
    val deltaHorometro: Double,
    val tieneFallaCritica: Boolean
)

/**
 * Lógica matemática pura para el cálculo de horas y desgaste mecánico de maquinaria.
 * Implementa las reglas canónicas de negocio y los mensajes del catálogo oficial.
 * Puro Kotlin (Clean Architecture).
 */
object CalculadoraJornada {

    private const val MILISEGUNDOS_POR_HORA = 3_600_000.0

    fun redondearUnDecimal(valor: Double): Double =
        round(valor * 10.0) / 10.0

    /**
     * Valida y calcula las métricas obligatorias para el cierre de una jornada de trabajo.
     * Retorna [AppResult.Success] con las métricas computadas o [AppResult.Error] con el mensaje canónico.
     */
    fun calcularCierre(
        horaInicioMillis: Long,
        horaSalidaMillis: Long,
        horometroInicial: Double,
        horometroFinal: Double,
        deducciones: List<Deduccion>
    ): AppResult<CalculoCierreJornada> {
        // 1. Regla: horaSalida > horaInicio (MSG-11)
        if (horaSalidaMillis <= horaInicioMillis) {
            return AppResult.Error(AppMessage.MSG_11)
        }

        // 2. Regla: horometroFinal > horometroInicial (MSG-05)
        if (horometroFinal <= horometroInicial) {
            return AppResult.Error(AppMessage.MSG_05)
        }

        // 3. Regla: deducción por falla técnica exige descripción >= 10 caracteres (MSG-06)
        for (deduccion in deducciones) {
            if (deduccion.esFalla) {
                val desc = deduccion.descripcion?.trim()
                if (desc == null || desc.length < 10) {
                    return AppResult.Error(AppMessage.MSG_06)
                }
            }
        }

        // 4. Cálculo de Horas Brutas
        val duracionHoras = (horaSalidaMillis - horaInicioMillis) / MILISEGUNDOS_POR_HORA
        val horasBrutas = redondearUnDecimal(duracionHoras)

        // 5. Cálculo de Horas Restadas
        val totalRestadas = deducciones.sumOf { it.duracionHoras }
        val horasRestadas = redondearUnDecimal(totalRestadas)

        // 6. Regla: horasRestadas < horasBrutas (MSG-12)
        if (horasRestadas >= horasBrutas) {
            return AppResult.Error(AppMessage.MSG_12)
        }

        // 7. Horas Efectivas = Horas Brutas - Horas Restadas
        val horasEfectivas = redondearUnDecimal(horasBrutas - horasRestadas)

        // 8. Delta Horómetro = Horómetro Final - Horómetro Inicial
        val deltaHorometro = redondearUnDecimal(horometroFinal - horometroInicial)

        // 9. Detección de Falla Crítica
        val tieneFallaCritica = deducciones.any { it.esFalla && it.severidad == SeveridadFalla.CRITICA }

        return AppResult.Success(
            CalculoCierreJornada(
                horasBrutas = horasBrutas,
                horasRestadas = horasRestadas,
                horasEfectivas = horasEfectivas,
                deltaHorometro = deltaHorometro,
                tieneFallaCritica = tieneFallaCritica
            )
        )
    }
}
