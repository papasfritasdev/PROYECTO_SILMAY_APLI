import { esEstadoMaquina, esTipoEquipo, type Maquina } from '../../domain/model/Maquina';

const numero = (valor: unknown): number | null =>
  typeof valor === 'number' && Number.isFinite(valor) ? valor : null;

/** Mapea `maquinaria/{codigoInterno}` (docs/firebase-schema.md §2.5). Null si está incompleto. */
export function mapearMaquina(id: string, datos: Readonly<Record<string, unknown>>): Maquina | null {
  const tipoEquipo = datos['tipoEquipo'];
  const estado = datos['estado'];
  const horometroBase = numero(datos['horometroBase']);
  const horometroActual = numero(datos['horometroActual']);
  const intervalo = numero(datos['intervaloMantenimiento']);
  const proximo = numero(datos['proximoMantenimientoHoras']);
  const placa = datos['placa'];

  if (
    !esTipoEquipo(tipoEquipo) ||
    !esEstadoMaquina(estado) ||
    horometroBase === null ||
    horometroActual === null ||
    intervalo === null ||
    proximo === null
  ) {
    return null;
  }

  return {
    codigoInterno: id,
    placa: typeof placa === 'string' && placa.length > 0 ? placa : null,
    marca: typeof datos['marca'] === 'string' ? datos['marca'] : '',
    modelo: typeof datos['modelo'] === 'string' ? datos['modelo'] : '',
    tipoEquipo,
    horometroBase,
    horometroActual,
    intervaloMantenimiento: intervalo,
    proximoMantenimientoHoras: proximo,
    estado
  };
}
