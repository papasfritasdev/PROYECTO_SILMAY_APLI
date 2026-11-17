package ec.silmay.app.domain.repository

import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Jornada

/**
 * Contrato de Dominio para la persistencia y consulta local de jornadas (HU-04).
 * Cero dependencias de Android SDK o Firebase SDK (Clean Architecture estricta).
 */
interface JornadaRepository {
    /**
     * Guarda o actualiza una jornada en disco de forma local-primero.
     * Retorna el UUID de la jornada persistida.
     */
    suspend fun guardarJornada(jornada: Jornada): AppResult<String>

    /**
     * Obtiene la jornada en estado 'abierta' perteneciente al usuario especificado.
     * Si no existe ninguna jornada abierta, retorna `null`.
     */
    suspend fun obtenerJornadaAbierta(userId: String): AppResult<Jornada?>

    /**
     * Obtiene una jornada específica por su UUID único.
     */
    suspend fun obtenerJornadaPorUuid(uuid: String): AppResult<Jornada?>

    /**
     * Obtiene el último horómetro registrado para la máquina indicada, para sugerirlo al operador.
     */
    suspend fun obtenerUltimoHorometroMaquina(maquinaId: String): AppResult<Double?>
}
