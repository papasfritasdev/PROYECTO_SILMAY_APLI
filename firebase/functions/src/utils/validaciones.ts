/**
 * Utilidades de validación para el Sistema SILMAY
 * docs/firebase-schema.md y docs/specs/sprint-01-specs.md (HT-04)
 */

export type RolUsuario = 'operador' | 'supervisor' | 'contador' | 'admin';

export const ROLES_VALIDOS: RolUsuario[] = ['operador', 'supervisor', 'contador', 'admin'];

export function esRolValido(rol: unknown): rol is RolUsuario {
  return typeof rol === 'string' && ROLES_VALIDOS.includes(rol as RolUsuario);
}

/**
 * Valida formato y dígito verificador de cédula ecuatoriana de 10 dígitos (Módulo 10).
 */
export function esCedulaEcuatorianaValida(cedula: unknown): boolean {
  if (typeof cedula !== 'string' || !/^\d{10}$/.test(cedula)) {
    return false;
  }

  const provincia = parseInt(cedula.substring(0, 2), 10);
  if ((provincia < 1 || provincia > 24) && provincia !== 30) {
    return false;
  }

  const tercerDigito = parseInt(cedula.charAt(2), 10);
  if (tercerDigito >= 6) {
    return false;
  }

  const coeficientes = [2, 1, 2, 1, 2, 1, 2, 1, 2];
  const digitoVerificador = parseInt(cedula.charAt(9), 10);

  let suma = 0;
  for (let i = 0; i < 9; i++) {
    let valor = parseInt(cedula.charAt(i), 10) * coeficientes[i];
    if (valor >= 10) {
      valor -= 9;
    }
    suma += valor;
  }

  const residuo = suma % 10;
  const verificadorCalculado = residuo === 0 ? 0 : 10 - residuo;

  return verificadorCalculado === digitoVerificador;
}

export function esEmailValido(email: unknown): boolean {
  if (typeof email !== 'string') return false;
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}
