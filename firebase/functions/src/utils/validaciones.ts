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

export const LONGITUD_MINIMA_CONTRASENA = 8;

/**
 * HU-02 CA1: contraseña mínima de 8 caracteres alfanuméricos.
 * Acuerdo del equipo (Sprint 2): exige al menos una letra y un dígito; los símbolos se permiten.
 * No admite espacios al inicio ni al final.
 */
export function esContrasenaValida(contrasena: unknown): boolean {
  if (typeof contrasena !== 'string') return false;
  if (contrasena !== contrasena.trim()) return false;
  return (
    contrasena.length >= LONGITUD_MINIMA_CONTRASENA &&
    /\p{L}/u.test(contrasena) &&
    /\d/.test(contrasena)
  );
}

/**
 * Teléfono ecuatoriano en formato nacional (`0…`) o internacional (`+593…`):
 * celular `9XXXXXXXX` o fijo `[2-7]XXXXXXX`. Se ignoran espacios y guiones.
 */
export function esTelefonoEcuatorianoValido(telefono: unknown): boolean {
  if (typeof telefono !== 'string') return false;
  const normalizado = telefono.replace(/[\s-]/g, '');
  return /^(?:0|\+593)(?:9\d{8}|[2-7]\d{7})$/.test(normalizado);
}
