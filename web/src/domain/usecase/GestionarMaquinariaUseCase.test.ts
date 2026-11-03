import { describe, expect, it } from 'vitest';
import { FakeMaquinariaRepository, maquinaDePrueba } from '../../test/FakeMaquinariaRepository';
import type { FormularioMaquina } from '../model/Maquina';
import { GestionarMaquinariaUseCase } from './GestionarMaquinariaUseCase';

const alta: FormularioMaquina = {
  codigoInterno: 'VOL-03',
  placa: 'tba1234',
  marca: 'Hino',
  modelo: '500',
  tipoEquipo: 'volqueta',
  horometroBase: '0',
  intervaloMantenimiento: '500'
};

describe('GestionarMaquinariaUseCase (HU-09)', () => {
  it('CA1: registra la unidad construida en el dominio', async () => {
    const repo = new FakeMaquinariaRepository();

    expect(await new GestionarMaquinariaUseCase(repo).registrar(alta)).toEqual({ ok: true });
    expect(repo.registradas[0]).toMatchObject({
      codigoInterno: 'VOL-03',
      placa: 'TBA-1234',
      proximoMantenimientoHoras: 500,
      estado: 'operativa'
    });
  });

  it('formularios inválidos no llegan al repositorio', async () => {
    const repo = new FakeMaquinariaRepository();
    expect(await new GestionarMaquinariaUseCase(repo).registrar({ ...alta, placa: '' })).toEqual({
      ok: false,
      camposInvalidos: ['placa'],
      mensaje: null
    });
    expect(repo.registradas).toHaveLength(0);
  });

  it('CA2: código o placa duplicados retornan MSG-09', async () => {
    const repo = new FakeMaquinariaRepository([maquinaDePrueba({ codigoInterno: 'VOL-03' })]);
    const useCase = new GestionarMaquinariaUseCase(repo);

    expect(await useCase.registrar(alta)).toEqual({ ok: false, camposInvalidos: [], mensaje: 'MSG-09' });

    const repoPlaca = new FakeMaquinariaRepository([maquinaDePrueba({ codigoInterno: 'VOL-01', placa: 'TBA-1234' })]);
    expect(await new GestionarMaquinariaUseCase(repoPlaca).registrar(alta)).toMatchObject({ mensaje: 'MSG-09' });
  });

  it('edita placa, marca y modelo conservando el estado', async () => {
    const maquina = maquinaDePrueba({ estado: 'requiere_mantenimiento' });
    const repo = new FakeMaquinariaRepository([maquina]);

    await new GestionarMaquinariaUseCase(repo).editar(maquina, { placa: '', marca: 'CAT', modelo: '330' });

    expect(repo.actualizaciones[0]).toEqual({
      codigo: 'EX-01',
      cambios: { placa: null, marca: 'CAT', modelo: '330', estado: 'requiere_mantenimiento' }
    });
  });

  it('una edición inválida no llega al repositorio', async () => {
    const repo = new FakeMaquinariaRepository([maquinaDePrueba()]);
    expect(
      await new GestionarMaquinariaUseCase(repo).editar(maquinaDePrueba(), { placa: 'x', marca: 'a', modelo: 'b' })
    ).toMatchObject({ ok: false, camposInvalidos: ['placa'] });
    expect(repo.actualizaciones).toHaveLength(0);
  });

  it('baja lógica y reactivación según el horómetro', async () => {
    const repo = new FakeMaquinariaRepository([maquinaDePrueba()]);
    const useCase = new GestionarMaquinariaUseCase(repo);

    await useCase.cambiarActividad(maquinaDePrueba(), false);
    await useCase.cambiarActividad(maquinaDePrueba({ estado: 'inactiva', horometroActual: 1300 }), true);

    expect(repo.actualizaciones.map((a) => a.cambios.estado)).toEqual(['inactiva', 'requiere_mantenimiento']);
  });

  it('propaga errores del servidor (MSG-13 por permisos)', async () => {
    const repo = new FakeMaquinariaRepository([maquinaDePrueba()]);
    repo.errorForzado = 'MSG-13';
    expect(await new GestionarMaquinariaUseCase(repo).cambiarActividad(maquinaDePrueba(), false)).toEqual({
      ok: false,
      camposInvalidos: [],
      mensaje: 'MSG-13'
    });
  });
});
