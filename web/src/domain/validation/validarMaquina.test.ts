import { describe, expect, it } from 'vitest';
import { maquinaDePrueba } from '../../test/FakeMaquinariaRepository';
import {
  calcularProximoMantenimiento,
  estadoAlReactivar,
  normalizarCodigo,
  normalizarPlaca,
  redondearUnDecimal
} from '../fleet/flota';
import type { FormularioMaquina } from '../model/Maquina';
import { construirMaquina, validarCambios } from './validarMaquina';

const formulario: FormularioMaquina = {
  codigoInterno: ' ex-01 ',
  placa: '',
  marca: ' Caterpillar ',
  modelo: '320D',
  tipoEquipo: 'excavadora',
  horometroBase: '1250.5',
  intervaloMantenimiento: '250'
};

describe('Fórmulas de flota', () => {
  it('HU-09 CA1: próximo mantenimiento = horómetro base + intervalo, a 1 decimal', () => {
    expect(calcularProximoMantenimiento(1250.5, 250)).toBe(1500.5);
    expect(calcularProximoMantenimiento(0.1, 0.2)).toBe(0.3);
    expect(redondearUnDecimal(12.345)).toBe(12.3);
  });

  it('normaliza código y placa a su forma canónica', () => {
    expect(normalizarCodigo(' ex-01 ')).toBe('EX-01');
    expect(normalizarPlaca('tba1234')).toBe('TBA-1234');
    expect(normalizarPlaca(' TBA-123 ')).toBe('TBA-123');
    expect(normalizarPlaca('TB-12345')).toBeNull();
    expect(normalizarPlaca('1234ABC')).toBeNull();
  });

  it('al reactivar vuelve a operativa salvo que el horómetro ya alcanzó el límite', () => {
    expect(estadoAlReactivar(maquinaDePrueba({ horometroActual: 1100 }))).toBe('operativa');
    expect(estadoAlReactivar(maquinaDePrueba({ horometroActual: 1250 }))).toBe('requiere_mantenimiento');
  });
});

describe('construirMaquina (HU-09 CA1)', () => {
  it('construye la unidad canónica: operativa, horometroActual = base y próximo calculado', () => {
    expect(construirMaquina(formulario)).toEqual({
      ok: true,
      maquina: {
        codigoInterno: 'EX-01',
        placa: null,
        marca: 'Caterpillar',
        modelo: '320D',
        tipoEquipo: 'excavadora',
        horometroBase: 1250.5,
        horometroActual: 1250.5,
        intervaloMantenimiento: 250,
        proximoMantenimientoHoras: 1500.5,
        estado: 'operativa'
      }
    });
  });

  it('acepta coma decimal en el horómetro y normaliza la placa', () => {
    const resultado = construirMaquina({ ...formulario, horometroBase: '10,5', placa: 'abc1234' });
    expect(resultado.ok && resultado.maquina.horometroBase).toBe(10.5);
    expect(resultado.ok && resultado.maquina.placa).toBe('ABC-1234');
  });

  it('una volqueta exige placa', () => {
    expect(construirMaquina({ ...formulario, tipoEquipo: 'volqueta' })).toEqual({
      ok: false,
      camposInvalidos: ['placa']
    });
  });

  it('detecta campos inválidos', () => {
    const resultado = construirMaquina({
      ...formulario,
      codigoInterno: '#',
      placa: 'XX',
      marca: ' ',
      modelo: '',
      horometroBase: '-1',
      intervaloMantenimiento: '0'
    });
    expect(resultado.ok ? [] : [...resultado.camposInvalidos].sort()).toEqual(
      ['codigoInterno', 'horometroBase', 'intervaloMantenimiento', 'marca', 'modelo', 'placa'].sort()
    );
  });

  it('rechaza horómetros con más de 1 decimal e intervalos no enteros', () => {
    expect(construirMaquina({ ...formulario, horometroBase: '10.55' })).toMatchObject({
      ok: false,
      camposInvalidos: ['horometroBase']
    });
    expect(construirMaquina({ ...formulario, intervaloMantenimiento: '250.5' })).toMatchObject({
      ok: false,
      camposInvalidos: ['intervaloMantenimiento']
    });
  });

  it('rechaza un tipo de equipo fuera del catálogo', () => {
    expect(
      construirMaquina({ ...formulario, tipoEquipo: 'grúa' as FormularioMaquina['tipoEquipo'] })
    ).toMatchObject({ ok: false, camposInvalidos: ['tipoEquipo'] });
  });
});

describe('validarCambios', () => {
  it('normaliza placa, marca y modelo', () => {
    expect(validarCambios(maquinaDePrueba(), { placa: 'pbc 0123', marca: ' CAT ', modelo: ' 330 ' })).toEqual({
      ok: true,
      cambios: { placa: 'PBC-0123', marca: 'CAT', modelo: '330' }
    });
  });

  it('no permite quitar la placa a una volqueta', () => {
    expect(
      validarCambios(maquinaDePrueba({ tipoEquipo: 'volqueta', placa: 'TBA-1234' }), {
        placa: '',
        marca: 'Hino',
        modelo: '500'
      })
    ).toEqual({ ok: false, camposInvalidos: ['placa'] });
  });

  it('marca y modelo son obligatorios', () => {
    expect(validarCambios(maquinaDePrueba(), { placa: '', marca: '', modelo: '' })).toEqual({
      ok: false,
      camposInvalidos: ['marca', 'modelo']
    });
  });
});
