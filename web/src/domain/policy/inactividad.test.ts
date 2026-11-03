import { describe, expect, it } from 'vitest';
import { debeCerrarSesion, LIMITE_INACTIVIDAD_MS } from './inactividad';

describe('Política de inactividad (HU-01b CA3, RNF-07)', () => {
  const inicio = 1_700_000_000_000;

  it('el límite oficial es de 30 minutos', () => {
    expect(LIMITE_INACTIVIDAD_MS).toBe(1_800_000);
  });

  it('no cierra antes de 30 minutos continuos sin actividad', () => {
    expect(debeCerrarSesion(inicio, inicio + LIMITE_INACTIVIDAD_MS - 1)).toBe(false);
  });

  it('cierra al cumplirse exactamente 30 minutos', () => {
    expect(debeCerrarSesion(inicio, inicio + LIMITE_INACTIVIDAD_MS)).toBe(true);
  });

  it('acepta un límite personalizado', () => {
    expect(debeCerrarSesion(inicio, inicio + 500, 400)).toBe(true);
    expect(debeCerrarSesion(inicio, inicio + 300, 400)).toBe(false);
  });
});
