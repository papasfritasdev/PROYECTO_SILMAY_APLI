/**
 * Catálogo Oficial de Mensajes del Sistema SILMAY (docs/messages-catalog.md).
 * Inmutable: cualquier texto mostrado al usuario debe provenir de aquí.
 * La prueba `messages.test.ts` verifica la correspondencia literal con el documento.
 */
export const APP_MESSAGES = {
  'MSG-01': 'Credenciales no válidas. Revise su usuario o contraseña.',
  'MSG-02': 'Se requiere conexión a Internet para el registro inicial de su cuenta en este dispositivo.',
  'MSG-03': 'Cuenta deshabilitada. Contacte con la administración de SILMAY.',
  'MSG-04': 'Memoria insuficiente en el dispositivo para guardar el registro.',
  'MSG-05':
    'El horómetro final no puede ser menor al horómetro inicial. Por favor verifique el tablero de la máquina.',
  'MSG-06': 'La descripción de la novedad debe tener al menos 10 caracteres.',
  'MSG-07': 'Ingrese un valor monetario válido.',
  'MSG-08':
    'No se encontraron despachos ni movimientos operativos registrados en el período seleccionado.',
  'MSG-09': 'Ya existe una unidad de maquinaria registrada con ese código/placa.',
  'MSG-10': 'No existen movimientos que coincidan con los filtros seleccionados.',
  'MSG-11': 'La hora de salida debe ser posterior a la hora de inicio de la jornada.',
  'MSG-12': 'Las horas restadas no pueden superar la duración de la jornada.',
  'MSG-13': 'Esta plataforma no está habilitada para su rol.',
  'MSG-14': 'Tiene una jornada abierta. Finalícela antes de iniciar otra.',
  'MSG-15': 'Registre un comentario para marcar el registro como Observado.',
  'MSG-16': 'Ya existe un registro con ese valor en el catálogo.',
  'MSG-17': 'Ya existe un usuario registrado con ese correo o cédula.',
  'MSG-18': 'No se pudo completar la operación. Intente nuevamente.'
} as const;

export type CodigoMensaje = keyof typeof APP_MESSAGES;

export function textoMensaje(codigo: CodigoMensaje): string {
  return APP_MESSAGES[codigo];
}
