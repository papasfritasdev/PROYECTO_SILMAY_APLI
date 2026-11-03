/**
 * Roles autoritativos del Sistema SILMAY (docs/firebase-schema.md §2.1, ADR-0006).
 * Asignados como Custom Claim `role` por la Cloud Function gestionarUsuario.
 */
export const ROLES = ['operador', 'supervisor', 'contador', 'admin'] as const;

export type Rol = (typeof ROLES)[number];

/** RBAC web (HU-01b CA1/CA2): solo los roles administrativos operan la plataforma web. */
export const ROLES_WEB: readonly Rol[] = ['contador', 'admin'];

export function esRol(valor: unknown): valor is Rol {
  return typeof valor === 'string' && (ROLES as readonly string[]).includes(valor);
}

export function habilitadoEnWeb(rol: Rol): boolean {
  return ROLES_WEB.includes(rol);
}
