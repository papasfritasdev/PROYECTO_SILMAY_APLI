package ec.silmay.app.domain.sync

/**
 * Validación del formato UUID v4 exigido como Document ID (ADR-0005, RNF-11).
 * Puro Kotlin.
 */
object UuidV4 {
    private val PATRON =
        Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

    fun esValido(valor: String): Boolean = PATRON.matches(valor)
}

/** Fuente de identificadores UUID v4 criptográficos (inyectable para pruebas). */
fun interface GeneradorUuid {
    fun nuevo(): String
}

/** Reloj del dispositivo para `createdAtClient` (inyectable para pruebas). */
fun interface Reloj {
    fun ahoraMillis(): Long
}
