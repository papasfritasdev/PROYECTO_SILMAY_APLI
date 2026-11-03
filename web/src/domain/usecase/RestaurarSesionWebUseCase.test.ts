import { describe, expect, it } from 'vitest';
import { FakeAuthRepository, sesionDePrueba } from '../../test/FakeAuthRepository';
import { RestaurarSesionWebUseCase } from './RestaurarSesionWebUseCase';

describe('RestaurarSesionWebUseCase', () => {
  it('sin sesión persistida retorna null', async () => {
    const repo = new FakeAuthRepository();
    expect(await new RestaurarSesionWebUseCase(repo).ejecutar()).toBeNull();
    expect(repo.cierresDeSesion).toBe(0);
  });

  it('restaura la sesión de un admin activo', async () => {
    const sesion = sesionDePrueba('admin');
    const repo = FakeAuthRepository.conSesionPersistida(sesion);
    expect(await new RestaurarSesionWebUseCase(repo).ejecutar()).toEqual(sesion);
  });

  it('descarta y cierra sesiones de roles de campo o dadas de baja', async () => {
    const repoOperador = FakeAuthRepository.conSesionPersistida(sesionDePrueba('operador'));
    expect(await new RestaurarSesionWebUseCase(repoOperador).ejecutar()).toBeNull();
    expect(repoOperador.cierresDeSesion).toBe(1);

    const repoBaja = FakeAuthRepository.conSesionPersistida(sesionDePrueba('admin', false));
    expect(await new RestaurarSesionWebUseCase(repoBaja).ejecutar()).toBeNull();
    expect(repoBaja.cierresDeSesion).toBe(1);
  });
});
