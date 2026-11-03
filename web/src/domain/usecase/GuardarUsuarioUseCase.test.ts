import { describe, expect, it } from 'vitest';
import { FakeUsuariosRepository, usuarioDePrueba } from '../../test/FakeUsuariosRepository';
import type { DatosUsuario } from '../model/Usuario';
import { camposBloqueadosParaAutor, datosDesdeUsuario, GuardarUsuarioUseCase } from './GuardarUsuarioUseCase';

const ADMIN = 'uid-admin';

const alta: DatosUsuario = {
  nombres: ' Juan Pérez ',
  cedula: '1710034065',
  telefono: '0991234567',
  email: 'Juan@Silmay.ec',
  password: 'Silmay2026',
  rol: 'operador',
  tipoLicencia: null,
  zonaAsignada: 'cantera-banos',
  cargo: 'Operador',
  estado: true
};

describe('GuardarUsuarioUseCase (HU-02)', () => {
  it('CA1: un alta válida se envía normalizada al repositorio', async () => {
    const repo = new FakeUsuariosRepository();

    const resultado = await new GuardarUsuarioUseCase(repo).ejecutar(alta, ADMIN);

    expect(resultado.ok).toBe(true);
    expect(repo.guardados[0]).toMatchObject({ nombres: 'Juan Pérez', email: 'juan@silmay.ec' });
  });

  it('con campos inválidos no invoca al servidor', async () => {
    const repo = new FakeUsuariosRepository();

    const resultado = await new GuardarUsuarioUseCase(repo).ejecutar({ ...alta, cedula: '1' }, ADMIN);

    expect(resultado).toEqual({ ok: false, camposInvalidos: ['cedula'], mensaje: null });
    expect(repo.guardados).toHaveLength(0);
  });

  it('CA2: propaga MSG-17 cuando el servidor detecta un duplicado', async () => {
    const repo = new FakeUsuariosRepository();
    repo.errorAlGuardar = 'MSG-17';

    const resultado = await new GuardarUsuarioUseCase(repo).ejecutar(alta, ADMIN);

    expect(resultado).toEqual({ ok: false, camposInvalidos: [], mensaje: 'MSG-17' });
  });

  it('CA3: la baja lógica conserva el perfil y solo cambia el estado', async () => {
    const usuario = usuarioDePrueba();
    const repo = new FakeUsuariosRepository([usuario]);

    const resultado = await new GuardarUsuarioUseCase(repo).cambiarEstado(usuario, false, ADMIN);

    expect(resultado.ok).toBe(true);
    expect(repo.guardados[0]).toMatchObject({ uid: usuario.uid, cedula: usuario.cedula, estado: false });
    expect(repo.guardados[0].password).toBeUndefined();
  });

  it('autoprotección: el admin no puede darse de baja ni retirarse el rol', async () => {
    const propio = usuarioDePrueba({ uid: ADMIN, rol: 'admin' });
    const repo = new FakeUsuariosRepository([propio]);
    const useCase = new GuardarUsuarioUseCase(repo);

    expect(await useCase.cambiarEstado(propio, false, ADMIN)).toMatchObject({ ok: false, camposInvalidos: ['estado'] });
    expect(
      await useCase.ejecutar(datosDesdeUsuario(propio, { rol: 'contador' }), ADMIN)
    ).toMatchObject({ ok: false, camposInvalidos: ['rol'] });
    expect(repo.guardados).toHaveLength(0);
  });

  it('camposBloqueadosParaAutor no afecta a otros usuarios', () => {
    expect(camposBloqueadosParaAutor({ ...alta, uid: 'otro', estado: false, rol: 'operador' }, ADMIN)).toEqual([]);
  });
});
