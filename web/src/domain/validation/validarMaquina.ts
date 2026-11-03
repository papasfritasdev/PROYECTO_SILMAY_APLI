import { calcularProximoMantenimiento, normalizarCodigo, normalizarPlaca } from '../fleet/flota';
import { esTipoEquipo, type CambiosMaquina, type FormularioMaquina, type Maquina } from '../model/Maquina';

export type CampoMaquina =
  | 'codigoInterno'
  | 'placa'
  | 'marca'
  | 'modelo'
  | 'tipoEquipo'
  | 'horometroBase'
  | 'intervaloMantenimiento';

const noVacio = (valor: string): boolean => valor.trim().length > 0;

/** Número ≥ 0 con como máximo 1 decimal (horómetros, business-formulas §5). */
function horometroValido(valor: string): number | null {
  const texto = valor.trim().replace(',', '.');
  if (!/^\d+(\.\d)?$/.test(texto)) return null;
  return Number(texto);
}

/** Intervalo preventivo en horas enteras positivas (p. ej. 250 o 500). */
function intervaloValido(valor: string): number | null {
  const texto = valor.trim();
  if (!/^\d+$/.test(texto)) return null;
  const numero = Number(texto);
  return numero > 0 ? numero : null;
}

/** Placa: obligatoria en volquetas; si se informa, debe tener formato `ABC-1234`. */
function placaValida(placa: string, tipo: string): boolean {
  if (!noVacio(placa)) return tipo !== 'volqueta';
  return normalizarPlaca(placa) !== null;
}

export type ResultadoConstruccion =
  | { readonly ok: true; readonly maquina: Maquina }
  | { readonly ok: false; readonly camposInvalidos: readonly CampoMaquina[] };

/**
 * HU-09 CA1: valida el formulario y construye la unidad canónica con
 * `horometroActual = horometroBase`, `proximoMantenimientoHoras = base + intervalo`
 * y `estado: 'operativa'` (coherente con las Security Rules de HT-05).
 */
export function construirMaquina(formulario: FormularioMaquina): ResultadoConstruccion {
  const invalidos: CampoMaquina[] = [];
  const codigo = normalizarCodigo(formulario.codigoInterno);
  const horometroBase = horometroValido(formulario.horometroBase);
  const intervalo = intervaloValido(formulario.intervaloMantenimiento);

  if (!/^[A-Z0-9][A-Z0-9-]*$/.test(codigo)) invalidos.push('codigoInterno');
  if (!esTipoEquipo(formulario.tipoEquipo)) invalidos.push('tipoEquipo');
  if (!placaValida(formulario.placa, formulario.tipoEquipo)) invalidos.push('placa');
  if (!noVacio(formulario.marca)) invalidos.push('marca');
  if (!noVacio(formulario.modelo)) invalidos.push('modelo');
  if (horometroBase === null) invalidos.push('horometroBase');
  if (intervalo === null) invalidos.push('intervaloMantenimiento');

  if (invalidos.length > 0 || horometroBase === null || intervalo === null) {
    return { ok: false, camposInvalidos: invalidos };
  }

  return {
    ok: true,
    maquina: {
      codigoInterno: codigo,
      placa: noVacio(formulario.placa) ? normalizarPlaca(formulario.placa) : null,
      marca: formulario.marca.trim(),
      modelo: formulario.modelo.trim(),
      tipoEquipo: formulario.tipoEquipo,
      horometroBase,
      horometroActual: horometroBase,
      intervaloMantenimiento: intervalo,
      proximoMantenimientoHoras: calcularProximoMantenimiento(horometroBase, intervalo),
      estado: 'operativa'
    }
  };
}

/** Valida y normaliza los cambios de una unidad existente. */
export function validarCambios(
  maquina: Maquina,
  cambios: { readonly placa: string; readonly marca: string; readonly modelo: string }
): { readonly ok: true; readonly cambios: Omit<CambiosMaquina, 'estado'> } | {
  readonly ok: false;
  readonly camposInvalidos: readonly CampoMaquina[];
} {
  const invalidos: CampoMaquina[] = [];
  if (!placaValida(cambios.placa, maquina.tipoEquipo)) invalidos.push('placa');
  if (!noVacio(cambios.marca)) invalidos.push('marca');
  if (!noVacio(cambios.modelo)) invalidos.push('modelo');
  if (invalidos.length > 0) return { ok: false, camposInvalidos: invalidos };

  return {
    ok: true,
    cambios: {
      placa: noVacio(cambios.placa) ? normalizarPlaca(cambios.placa) : null,
      marca: cambios.marca.trim(),
      modelo: cambios.modelo.trim()
    }
  };
}
