import { useEffect, useRef } from 'react';
import { debeCerrarSesion, LIMITE_INACTIVIDAD_MS } from '../domain/policy/inactividad';

/** Eventos que cuentan como interacción del usuario (HU-01b CA3: teclado o ratón). */
export const EVENTOS_ACTIVIDAD = [
  'mousemove',
  'mousedown',
  'keydown',
  'wheel',
  'touchstart',
  'scroll'
] as const;

export interface OpcionesInactividad {
  limiteMs?: number;
  intervaloVerificacionMs?: number;
}

/**
 * Ejecuta `onExpirar` tras `limiteMs` continuos sin interacción mientras `activo` sea true.
 * La verificación periódica compara contra el reloj real (ver domain/policy/inactividad.ts).
 */
export function useInactividad(
  activo: boolean,
  onExpirar: () => void,
  { limiteMs = LIMITE_INACTIVIDAD_MS, intervaloVerificacionMs = 15_000 }: OpcionesInactividad = {}
): void {
  const onExpirarRef = useRef(onExpirar);
  onExpirarRef.current = onExpirar;

  useEffect(() => {
    if (!activo) return undefined;

    let ultimaActividad = Date.now();
    const registrarActividad = (): void => {
      ultimaActividad = Date.now();
    };

    EVENTOS_ACTIVIDAD.forEach((evento) =>
      window.addEventListener(evento, registrarActividad, { passive: true })
    );

    const verificador = window.setInterval(() => {
      if (debeCerrarSesion(ultimaActividad, Date.now(), limiteMs)) {
        window.clearInterval(verificador);
        onExpirarRef.current();
      }
    }, intervaloVerificacionMs);

    return () => {
      window.clearInterval(verificador);
      EVENTOS_ACTIVIDAD.forEach((evento) =>
        window.removeEventListener(evento, registrarActividad)
      );
    };
  }, [activo, limiteMs, intervaloVerificacionMs]);
}
