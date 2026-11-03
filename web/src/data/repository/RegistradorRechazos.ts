import { httpsCallable, type Functions } from 'firebase/functions';

export type OperacionRechazada = 'read' | 'create' | 'update' | 'delete';

export interface RegistradorRechazos {
  registrar(coleccion: string, documentoId: string, operacion: OperacionRechazada): Promise<void>;
}

/**
 * Audita rechazos PERMISSION_DENIED vía la callable `registrarRechazo` (HU-09 CA3).
 * Es de mejor esfuerzo: si la auditoría falla, no debe ocultar el error original al usuario.
 */
export class RegistradorRechazosFirebase implements RegistradorRechazos {
  constructor(private readonly functions: Functions) {}

  async registrar(coleccion: string, documentoId: string, operacion: OperacionRechazada): Promise<void> {
    try {
      await httpsCallable(this.functions, 'registrarRechazo')({
        coleccion,
        documentoId,
        operacion,
        origen: 'web'
      });
    } catch {
      // Mejor esfuerzo: la regla ya bloqueó la operación; no se propaga el fallo de auditoría.
    }
  }
}
