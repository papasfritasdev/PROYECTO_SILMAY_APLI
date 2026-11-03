/**
 * Política de cierre de sesión por inactividad (HU-01b CA3, RNF-07).
 * Se compara contra el reloj real (no contra temporizadores), por lo que también
 * cierra correctamente si el navegador suspendió la pestaña.
 */
export const LIMITE_INACTIVIDAD_MS = 30 * 60 * 1000;

export function debeCerrarSesion(
  ultimaActividadMs: number,
  ahoraMs: number,
  limiteMs: number = LIMITE_INACTIVIDAD_MS
): boolean {
  return ahoraMs - ultimaActividadMs >= limiteMs;
}
