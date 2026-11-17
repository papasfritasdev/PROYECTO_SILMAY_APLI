package ec.silmay.app.domain.model

/**
 * Entidad de dominio que representa un turno de maquinaria pesada (docs/firebase-schema.md §2.2).
 * Colección: jornadas
 * Puro Kotlin (Clean Architecture).
 */
data class Jornada(
    val uuid: String,
    val maquinaId: String,
    val lugarId: String,
    val horaInicioMillis: Long,
    val horaSalidaMillis: Long? = null,
    val horometroInicial: Double,
    val horometroFinal: Double? = null,
    val horasBrutas: Double? = null,
    val horasRestadas: Double = 0.0,
    val horasEfectivas: Double? = null,
    val deltaHorometro: Double? = null,
    val observaciones: String? = null,
    val estado: EstadoJornada = EstadoJornada.ABIERTA,
    val tieneFallaCritica: Boolean = false,
    val notificadaFalla: Boolean = false,
    val procesada: Boolean = false,
    val deducciones: List<Deduccion> = emptyList(),
    // Metadatos de control obligatorios (docs/firebase-schema.md §1)
    val userId: String,
    val deviceId: String,
    val zona: String,
    val createdAtClientMillis: Long = System.currentTimeMillis()
) {
    val estaAbierta: Boolean get() = estado == EstadoJornada.ABIERTA
    val estaCerrada: Boolean get() = estado == EstadoJornada.CERRADA
}
