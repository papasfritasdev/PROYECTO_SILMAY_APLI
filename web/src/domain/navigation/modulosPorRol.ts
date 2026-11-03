import type { Rol } from '../model/Rol';

/** Módulos web (docs/architecture.md §2: M5, M6, M7, M8). */
export const MODULOS_WEB = [
  'monitoreo',
  'validacion',
  'reporteria',
  'alertas',
  'usuarios',
  'maquinaria',
  'catalogos'
] as const;

export type ModuloWeb = (typeof MODULOS_WEB)[number];

/**
 * Matriz RBAC de la plataforma web (HU-01b CA1), derivada de las specs:
 * - admin: HU-02 usuarios, HU-09 maquinaria, HU-16 catálogos, HU-11 alertas, HU-12/14 reportería, HU-15 monitoreo.
 * - contador: HU-15 monitoreo, HU-13 validación, HU-11 alertas (solo lectura), HU-12/14 reportería.
 */
const MATRIZ_RBAC_WEB: Readonly<Record<'admin' | 'contador', readonly ModuloWeb[]>> = {
  admin: ['monitoreo', 'reporteria', 'alertas', 'usuarios', 'maquinaria', 'catalogos'],
  contador: ['monitoreo', 'validacion', 'reporteria', 'alertas']
};

export function modulosPorRol(rol: Rol): readonly ModuloWeb[] {
  return rol === 'admin' || rol === 'contador' ? MATRIZ_RBAC_WEB[rol] : [];
}

export function puedeAccederModulo(rol: Rol, modulo: ModuloWeb): boolean {
  return modulosPorRol(rol).includes(modulo);
}
