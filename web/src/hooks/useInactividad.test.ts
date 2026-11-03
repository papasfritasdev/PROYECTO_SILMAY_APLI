import { renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { LIMITE_INACTIVIDAD_MS } from '../domain/policy/inactividad';
import { useInactividad } from './useInactividad';

describe('useInactividad (HU-01b CA3)', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('expira tras 30 minutos continuos sin interacción', () => {
    const onExpirar = vi.fn();
    renderHook(() => useInactividad(true, onExpirar));

    vi.advanceTimersByTime(LIMITE_INACTIVIDAD_MS - 15_000);
    expect(onExpirar).not.toHaveBeenCalled();

    vi.advanceTimersByTime(15_000);
    expect(onExpirar).toHaveBeenCalledTimes(1);
  });

  it('la actividad de teclado o ratón reinicia el conteo', () => {
    const onExpirar = vi.fn();
    renderHook(() => useInactividad(true, onExpirar));

    vi.advanceTimersByTime(20 * 60 * 1000);
    window.dispatchEvent(new KeyboardEvent('keydown'));
    vi.advanceTimersByTime(20 * 60 * 1000);
    window.dispatchEvent(new MouseEvent('mousemove'));
    vi.advanceTimersByTime(20 * 60 * 1000);

    expect(onExpirar).not.toHaveBeenCalled();

    vi.advanceTimersByTime(10 * 60 * 1000);
    expect(onExpirar).toHaveBeenCalledTimes(1);
  });

  it('no hace nada si no hay sesión activa', () => {
    const onExpirar = vi.fn();
    renderHook(() => useInactividad(false, onExpirar));

    vi.advanceTimersByTime(2 * LIMITE_INACTIVIDAD_MS);
    expect(onExpirar).not.toHaveBeenCalled();
  });

  it('expira una sola vez y libera los listeners al desmontar', () => {
    const onExpirar = vi.fn();
    const quitar = vi.spyOn(window, 'removeEventListener');
    const { unmount } = renderHook(() => useInactividad(true, onExpirar));

    vi.advanceTimersByTime(3 * LIMITE_INACTIVIDAD_MS);
    expect(onExpirar).toHaveBeenCalledTimes(1);

    unmount();
    expect(quitar).toHaveBeenCalledWith('keydown', expect.any(Function));
  });
});
