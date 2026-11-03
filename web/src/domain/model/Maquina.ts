/** Inventario unificado de equipos y volquetas (docs/firebase-schema.md §2.5). */
export const TIPOS_EQUIPO = [
  'excavadora',
  'cargadora',
  'retroexcavadora',
  'tractocamión',
  'volqueta'
] as const;

export type TipoEquipo = (typeof TIPOS_EQUIPO)[number];

export const ESTADOS_MAQUINA = ['operativa', 'requiere_mantenimiento', 'inactiva'] as const;

export type EstadoMaquina = (typeof ESTADOS_MAQUINA)[number];

export interface Maquina {
  /** PK y Document ID (unicidad atómica, HU-09 CA2). */
  readonly codigoInterno: string;
  readonly placa: string | null;
  readonly marca: string;
  readonly modelo: string;
  readonly tipoEquipo: TipoEquipo;
  readonly horometroBase: number;
  readonly horometroActual: number;
  readonly intervaloMantenimiento: number;
  readonly proximoMantenimientoHoras: number;
  readonly estado: EstadoMaquina;
}

/** Valores del formulario de alta tal como los escribe el usuario. */
export interface FormularioMaquina {
  readonly codigoInterno: string;
  readonly placa: string;
  readonly marca: string;
  readonly modelo: string;
  readonly tipoEquipo: TipoEquipo;
  readonly horometroBase: string;
  readonly intervaloMantenimiento: string;
}

/** Campos editables de una unidad existente (el código, horómetros e intervalo no se editan). */
export interface CambiosMaquina {
  readonly placa: string | null;
  readonly marca: string;
  readonly modelo: string;
  readonly estado: EstadoMaquina;
}

export function esTipoEquipo(valor: unknown): valor is TipoEquipo {
  return typeof valor === 'string' && (TIPOS_EQUIPO as readonly string[]).includes(valor);
}

export function esEstadoMaquina(valor: unknown): valor is EstadoMaquina {
  return typeof valor === 'string' && (ESTADOS_MAQUINA as readonly string[]).includes(valor);
}
