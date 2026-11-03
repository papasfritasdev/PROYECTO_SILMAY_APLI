import { describe, expect, it } from 'vitest';
import { mapearMaquina } from './maquinaMapper';

const documento = {
  codigoInterno: 'EX-01',
  placa: null,
  marca: 'Caterpillar',
  modelo: '320D',
  tipoEquipo: 'excavadora',
  horometroBase: 1000,
  horometroActual: 1100.5,
  intervaloMantenimiento: 250,
  proximoMantenimientoHoras: 1250,
  estado: 'operativa'
};

describe('mapearMaquina', () => {
  it('usa el Document ID como codigoInterno y mapea los campos numéricos', () => {
    expect(mapearMaquina('EX-01', documento)).toMatchObject({
      codigoInterno: 'EX-01',
      placa: null,
      horometroActual: 1100.5,
      estado: 'operativa'
    });
  });

  it('descarta documentos con tipo, estado u horómetros inválidos', () => {
    expect(mapearMaquina('X', { ...documento, tipoEquipo: 'grúa' })).toBeNull();
    expect(mapearMaquina('X', { ...documento, estado: 'rota' })).toBeNull();
    expect(mapearMaquina('X', { ...documento, horometroActual: '1100' })).toBeNull();
    expect(mapearMaquina('X', { ...documento, proximoMantenimientoHoras: Number.NaN })).toBeNull();
  });

  it('normaliza placa vacía y textos ausentes', () => {
    expect(mapearMaquina('X', { ...documento, placa: '', marca: undefined })).toMatchObject({ placa: null, marca: '' });
  });
});
