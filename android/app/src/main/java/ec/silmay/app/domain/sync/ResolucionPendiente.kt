package ec.silmay.app.domain.sync

/**
 * Resultado de conciliar un registro que estaba en la cola local de sincronización,
 * una vez que el SDK terminó de transmitir las mutaciones pendientes (HT-07 §4).
 * Puro Kotlin.
 */
enum class ResolucionPendiente {
    /** El servidor aceptó la escritura: se retira de la bitácora. */
    CONFIRMADO,

    /** Las Security Rules rechazaron la escritura (PERMISSION_DENIED): se audita y se retira. */
    RECHAZADO,

    /** No se pudo determinar (sin red, error transitorio): se reintenta luego. */
    INDETERMINADO;

    companion object {
        /**
         * @param existeEnServidor resultado de leer el documento desde el servidor;
         *        `null` si la lectura no se completó.
         * @param lecturaDenegada la lectura fue rechazada con PERMISSION_DENIED. Las reglas
         *        solo permiten leer registros propios existentes, así que un registro propio
         *        ilegible tras vaciar la cola no llegó a crearse.
         */
        fun resolver(existeEnServidor: Boolean?, lecturaDenegada: Boolean): ResolucionPendiente =
            when {
                lecturaDenegada -> RECHAZADO
                existeEnServidor == true -> CONFIRMADO
                existeEnServidor == false -> RECHAZADO
                else -> INDETERMINADO
            }
    }
}
