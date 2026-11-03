package ec.silmay.app.domain.model

/**
 * Catálogo Oficial de Mensajes del Sistema SILMAY (UI y Validaciones).
 * Correspondencia exacta con docs/messages-catalog.md (MSG-01 a MSG-16).
 * Puro Kotlin (Cero dependencias de Android SDK / UI).
 */
enum class AppMessage(val codigo: String, val texto: String) {
    MSG_01("MSG-01", "Credenciales no válidas. Revise su usuario o contraseña."),
    MSG_02("MSG-02", "Se requiere conexión a Internet para el registro inicial de su cuenta en este dispositivo."),
    MSG_03("MSG-03", "Cuenta deshabilitada. Contacte con la administración de SILMAY."),
    MSG_04("MSG-04", "Memoria insuficiente en el dispositivo para guardar el registro."),
    MSG_05("MSG-05", "El horómetro final no puede ser menor al horómetro inicial. Por favor verifique el tablero de la máquina."),
    MSG_06("MSG-06", "La descripción de la novedad debe tener al menos 10 caracteres."),
    MSG_07("MSG-07", "Ingrese un valor monetario válido."),
    MSG_08("MSG-08", "No se encontraron despachos ni movimientos operativos registrados en el período seleccionado."),
    MSG_09("MSG-09", "Ya existe una unidad de maquinaria registrada con ese código/placa."),
    MSG_10("MSG-10", "No existen movimientos que coincidan con los filtros seleccionados."),
    MSG_11("MSG-11", "La hora de salida debe ser posterior a la hora de inicio de la jornada."),
    MSG_12("MSG-12", "Las horas restadas no pueden superar la duración de la jornada."),
    MSG_13("MSG-13", "Esta plataforma no está habilitada para su rol."),
    MSG_14("MSG-14", "Tiene una jornada abierta. Finalícela antes de iniciar otra."),
    MSG_15("MSG-15", "Registre un comentario para marcar el registro como Observado."),
    MSG_16("MSG-16", "Ya existe un registro con ese valor en el catálogo.");

    companion object {
        fun desdeCodigo(codigo: String): AppMessage? =
            entries.find { it.codigo.equals(codigo, ignoreCase = true) }
    }
}
