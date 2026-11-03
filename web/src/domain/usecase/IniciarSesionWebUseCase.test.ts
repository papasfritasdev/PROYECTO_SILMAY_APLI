import { describe, expect, it } from 'vitest';
import { FakeAuthRepository, sesionDePrueba } from '../../test/FakeAuthRepository';
import { evaluarAccesoWeb, IniciarSesionWebUseCase } from './IniciarSesionWebUseCase';

describe('IniciarSesionWebUseCase (HU-01b)', () => {
  it('campos vacíos retornan MSG-01 sin invocar al repositorio', async () => {
    const repo = FakeAuthRepository.conLogin(sesionDePrueba('admin'));
    const useCase = new IniciarSesionWebUseCase(repo);

    expect(await useCase.ejecutar('   ', 'Clave2026')).toEqual({ ok: false, mensaje: 'MSG-01' });
    expect(await useCase.ejecutar('admin@silmay.ec', '')).toEqual({ ok: false, mensaje: 'MSG-01' });
    expect(repo.ultimasCredenciales).toBeNull();
  });

  it('recorta espacios del correo antes de autenticar', async () => {
    const repo = FakeAuthRepository.conLogin(sesionDePrueba('admin'));
    await new IniciarSesionWebUseCase(repo).ejecutar('  admin@silmay.ec  ', 'Clave2026');
    expect(repo.ultimasCredenciales?.email).toBe('admin@silmay.ec');
  });

  it.each(['admin', 'contador'] as const)('CA1: %s activo accede', async (rol) => {
    const sesion = sesionDePrueba(rol);
    const repo = FakeAuthRepository.conLogin(sesion);

    const resultado = await new IniciarSesionWebUseCase(repo).ejecutar(sesion.email, 'Clave2026');

    expect(resultado).toEqual({ ok: true, data: sesion });
    expect(repo.cierresDeSesion).toBe(0);
  });

  it.each(['operador', 'supervisor'] as const)(
    'CA2: %s es bloqueado con MSG-13 y su sesión se cierra',
    async (rol) => {
      const repo = FakeAuthRepository.conLogin(sesionDePrueba(rol));

      const resultado = await new IniciarSesionWebUseCase(repo).ejecutar(`${rol}@silmay.ec`, 'Clave2026');

      expect(resultado).toEqual({ ok: false, mensaje: 'MSG-13' });
      expect(repo.cierresDeSesion).toBe(1);
    }
  );

  it('baja lógica: cuenta con estado false recibe MSG-03 y se cierra la sesión', async () => {
    const repo = FakeAuthRepository.conLogin(sesionDePrueba('contador', false));

    const resultado = await new IniciarSesionWebUseCase(repo).ejecutar('contador@silmay.ec', 'Clave2026');

    expect(resultado).toEqual({ ok: false, mensaje: 'MSG-03' });
    expect(repo.cierresDeSesion).toBe(1);
  });

  it('propaga el error del repositorio (credenciales inválidas)', async () => {
    const repo = FakeAuthRepository.conError('MSG-01');
    const resultado = await new IniciarSesionWebUseCase(repo).ejecutar('x@silmay.ec', 'mala');
    expect(resultado).toEqual({ ok: false, mensaje: 'MSG-01' });
  });

  it('evaluarAccesoWeb prioriza la baja lógica sobre el rol', () => {
    expect(evaluarAccesoWeb(sesionDePrueba('operador', false))).toBe('MSG-03');
    expect(evaluarAccesoWeb(sesionDePrueba('admin'))).toBeNull();
  });
});
