import { esRol } from '../../domain/model/Rol';
import type { Usuario } from '../../domain/model/Usuario';
import type { UsuarioSesion } from '../../domain/model/UsuarioSesion';

const texto = (valor: unknown): string => (typeof valor === 'string' ? valor : '');

/** Mapea el perfil completo de `usuarios` para la administración (HU-02). */
export function mapearUsuario(
  uid: string,
  datos: Readonly<Record<string, unknown>>
): Usuario | null {
  const rol = datos['rol'];
  if (!esRol(rol) || typeof datos['email'] !== 'string') return null;
  const tipoLicencia = datos['tipoLicencia'];
  return {
    uid,
    nombres: texto(datos['nombres']),
    cedula: texto(datos['cedula']),
    telefono: texto(datos['telefono']),
    email: datos['email'],
    rol,
    tipoLicencia: typeof tipoLicencia === 'string' ? tipoLicencia : null,
    zonaAsignada: texto(datos['zonaAsignada']),
    cargo: texto(datos['cargo']),
    estado: datos['estado'] === true
  };
}

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
