import type { EstadoMaquina, Maquina } from '../model/Maquina';

/** Horómetros y horas con 1 solo decimal (docs/business-formulas.md §5). */
export function redondearUnDecimal(valor: number): number {
  return Math.round(valor * 10) / 10;
}

/** HU-09 CA1 / business-formulas §2.2: próximo umbral preventivo. */
export function calcularProximoMantenimiento(horometroBase: number, intervaloMantenimiento: number): number {
  return redondearUnDecimal(horometroBase + intervaloMantenimiento);
}

/** Código de flota canónico: sin espacios y en mayúsculas (p. ej. `ex-01` → `EX-01`). */
export function normalizarCodigo(codigo: string): string {
  return codigo.replace(/\s+/g, '').toUpperCase();
}

/**
 * Placa canónica `ABC-1234` (3 letras + 3 o 4 dígitos), para que `abc1234` y `ABC-1234`
 * se detecten como la misma placa (HU-09 CA2). Retorna null si no tiene formato válido.
 */
export function normalizarPlaca(placa: string): string | null {
  const limpia = placa.replace(/[\s-]/g, '').toUpperCase();
  const coincidencia = /^([A-Z]{3})(\d{3,4})$/.exec(limpia);
  return coincidencia ? `${coincidencia[1]}-${coincidencia[2]}` : null;
}

/**
 * Estado al reactivar una unidad dada de baja: vuelve a `operativa`, salvo que ya haya
 * alcanzado su límite preventivo (semáforo rojo, business-formulas §2.1).
 */
export function estadoAlReactivar(maquina: Maquina): EstadoMaquina {
  return maquina.horometroActual >= maquina.proximoMantenimientoHoras
    ? 'requiere_mantenimiento'
    : 'operativa';
}
