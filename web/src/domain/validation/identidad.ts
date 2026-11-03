/**
 * Validadores de identidad replicados de firebase/functions/src/utils/validaciones.ts
 * para validar en cliente antes de invocar gestionarUsuario. Deben mantenerse idénticos.
 */
export const LONGITUD_MINIMA_CONTRASENA = 8;

/** Cédula ecuatoriana de 10 dígitos (provincia 01–24 o 30, tercer dígito < 6, Módulo 10). */
export function esCedulaEcuatorianaValida(cedula: string): boolean {
  if (!/^\d{10}$/.test(cedula)) return false;

  const provincia = Number(cedula.slice(0, 2));
  if ((provincia < 1 || provincia > 24) && provincia !== 30) return false;
  if (Number(cedula[2]) >= 6) return false;

  const coeficientes = [2, 1, 2, 1, 2, 1, 2, 1, 2];
  const suma = coeficientes.reduce((acumulado, coeficiente, i) => {
    const producto = Number(cedula[i]) * coeficiente;
    return acumulado + (producto >= 10 ? producto - 9 : producto);
  }, 0);
  const residuo = suma % 10;
  const verificador = residuo === 0 ? 0 : 10 - residuo;

  return verificador === Number(cedula[9]);
}

export function esEmailValido(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

/** Celular `09XXXXXXXX` / fijo `0[2-7]XXXXXXX`, o con prefijo `+593`. Ignora espacios y guiones. */
export function esTelefonoEcuatorianoValido(telefono: string): boolean {
  return /^(?:0|\+593)(?:9\d{8}|[2-7]\d{7})$/.test(telefono.replace(/[\s-]/g, ''));
}

/** HU-02 CA1: ≥ 8 caracteres con al menos una letra y un dígito; símbolos permitidos. */
export function esContrasenaValida(contrasena: string): boolean {
  return (
    contrasena === contrasena.trim() &&
    contrasena.length >= LONGITUD_MINIMA_CONTRASENA &&
    /\p{L}/u.test(contrasena) &&
    /\d/.test(contrasena)
  );
}
