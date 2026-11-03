import type { CodigoMensaje } from '../constants/messages';

/** Resultado de una operación de dominio: éxito o error con mensaje normalizado del catálogo. */
export type Resultado<T> =
  | { readonly ok: true; readonly data: T }
  | { readonly ok: false; readonly mensaje: CodigoMensaje };

export function exito<T>(data: T): Resultado<T> {
  return { ok: true, data };
}

export function fallo<T = never>(mensaje: CodigoMensaje): Resultado<T> {
  return { ok: false, mensaje };
}
