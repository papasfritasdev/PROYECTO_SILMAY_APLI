import { esRol } from '../../domain/model/Rol';
import type { UsuarioSesion } from '../../domain/model/UsuarioSesion';

/**
 * Mapea un documento de `usuarios` (docs/firebase-schema.md §2.1) a la entidad de dominio.
 * El rol se toma con prioridad del Custom Claim (autoritativo, ADR-0003).
 * Retorna null si el perfil es incompleto o el rol es desconocido.
 */
export function mapearUsuarioSesion(
  uid: string,
  datos: Readonly<Record<string, unknown>> | undefined,
  claimRole: unknown
): UsuarioSesion | null {
  if (!datos) return null;

  const rol = esRol(claimRole) ? claimRole : datos['rol'];
  const email = datos['email'];
  const zonaAsignada = datos['zonaAsignada'];

  if (!esRol(rol) || typeof email !== 'string' || typeof zonaAsignada !== 'string') {
    return null;
  }

  return {
    uid,
    email,
    nombres: typeof datos['nombres'] === 'string' ? datos['nombres'] : '',
    rol,
    zonaAsignada,
    estado: datos['estado'] === true
  };
}
