/**
 * Integración del FirebaseAuthRepository real con Auth + Firestore Emulator y las
 * Security Rules de HT-05 (HU-01b). Ejecutar con: npm run test:integration
 */
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { IniciarSesionWebUseCase } from '../../domain/usecase/IniciarSesionWebUseCase';
import {
  CONTRASENA_PRUEBA,
  crearCuenta,
  crearInstancia,
  limpiarEmuladores,
  type InstanciaFirebase
} from '../../test/emuladores';
import { FirebaseAuthRepository } from './FirebaseAuthRepository';

let firebase: InstanciaFirebase;
let repo: FirebaseAuthRepository;

beforeEach(async () => {
  await limpiarEmuladores();
  firebase = crearInstancia();
  repo = new FirebaseAuthRepository(firebase.auth, firebase.db);
});

afterEach(async () => {
  await firebase.cerrar();
});

describe('FirebaseAuthRepository + Security Rules (emulador)', () => {
  it('CA1: un admin inicia sesión; el perfil se lee pese a las reglas RBAC', async () => {
    const { email } = await crearCuenta({ rol: 'admin' });

    const resultado = await new IniciarSesionWebUseCase(repo).ejecutar(email, CONTRASENA_PRUEBA);

    expect(resultado.ok).toBe(true);
    if (resultado.ok) {
      expect(resultado.data).toMatchObject({ email, rol: 'admin', estado: true });
    }
    expect((await repo.obtenerSesionActual())?.rol).toBe('admin');
  });

  it('CA2: un operador es rechazado con MSG-13 y queda sin sesión', async () => {
    const { email } = await crearCuenta({ rol: 'operador' });

    const resultado = await new IniciarSesionWebUseCase(repo).ejecutar(email, CONTRASENA_PRUEBA);

    expect(resultado).toEqual({ ok: false, mensaje: 'MSG-13' });
    expect(firebase.auth.currentUser).toBeNull();
  });

  it('baja lógica en Auth (gestionarUsuario) muestra MSG-03', async () => {
    const { email } = await crearCuenta({ rol: 'contador', deshabilitadaEnAuth: true });
    expect(await new IniciarSesionWebUseCase(repo).ejecutar(email, CONTRASENA_PRUEBA)).toEqual({
      ok: false,
      mensaje: 'MSG-03'
    });
  });

  it('baja lógica en Firestore (estado false) muestra MSG-03 y cierra la sesión', async () => {
    const { email } = await crearCuenta({ rol: 'contador', estado: false });
    expect(await new IniciarSesionWebUseCase(repo).ejecutar(email, CONTRASENA_PRUEBA)).toEqual({
      ok: false,
      mensaje: 'MSG-03'
    });
    expect(firebase.auth.currentUser).toBeNull();
  });

  it('contraseña incorrecta o usuario inexistente muestran MSG-01 opaco', async () => {
    const { email } = await crearCuenta({ rol: 'admin' });
    expect(await repo.iniciarSesion(email, 'Incorrecta99')).toEqual({ ok: false, mensaje: 'MSG-01' });
    expect(await repo.iniciarSesion('nadie@silmay.ec', CONTRASENA_PRUEBA)).toEqual({ ok: false, mensaje: 'MSG-01' });
  });
});
