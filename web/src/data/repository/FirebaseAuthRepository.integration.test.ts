/**
 * Integración del FirebaseAuthRepository real con Auth + Firestore Emulator y las
 * Security Rules de HT-05 (HU-01b). Ejecutar con: npm run test:integration
 */
import { deleteApp, initializeApp, type FirebaseApp } from 'firebase/app';
import { connectAuthEmulator, getAuth, type Auth } from 'firebase/auth';
import { connectFirestoreEmulator, getFirestore } from 'firebase/firestore';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import type { Rol } from '../../domain/model/Rol';
import { IniciarSesionWebUseCase } from '../../domain/usecase/IniciarSesionWebUseCase';
import { FirebaseAuthRepository } from './FirebaseAuthRepository';

const PROJECT_ID = 'silmay-dev';
const AUTH_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST ?? '127.0.0.1:9099';
const FIRESTORE_HOST = process.env.FIRESTORE_EMULATOR_HOST ?? '127.0.0.1:8080';
const CONTRASENA = 'Silmay2026';
const OWNER = { Authorization: 'Bearer owner', 'Content-Type': 'application/json' };

let app: FirebaseApp;
let auth: Auth;
let repo: FirebaseAuthRepository;

interface CuentaPrueba {
  rol: Rol;
  estado?: boolean;
  deshabilitadaEnAuth?: boolean;
}

async function crearCuenta({ rol, estado = true, deshabilitadaEnAuth = false }: CuentaPrueba): Promise<string> {
  const email = `${rol}-${estado}-${deshabilitadaEnAuth}@silmay.ec`;
  const alta = await fetch(
    `http://${AUTH_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo`,
    { method: 'POST', headers: OWNER, body: JSON.stringify({ email, password: CONTRASENA }) }
  );
  const { localId } = (await alta.json()) as { localId: string };

  await fetch(`http://${AUTH_HOST}/identitytoolkit.googleapis.com/v1/projects/${PROJECT_ID}/accounts:update`, {
    method: 'POST',
    headers: OWNER,
    body: JSON.stringify({
      localId,
      customAttributes: JSON.stringify({ role: rol }),
      disableUser: deshabilitadaEnAuth
    })
  });

  // Escritura con privilegio de owner (omite reglas), como lo haría gestionarUsuario.
  await fetch(
    `http://${FIRESTORE_HOST}/v1/projects/${PROJECT_ID}/databases/(default)/documents/usuarios/${localId}`,
    {
      method: 'PATCH',
      headers: OWNER,
      body: JSON.stringify({
        fields: {
          uid: { stringValue: localId },
          nombres: { stringValue: `Usuario ${rol}` },
          email: { stringValue: email },
          rol: { stringValue: rol },
          zonaAsignada: { stringValue: 'administracion' },
          estado: { booleanValue: estado }
        }
      })
    }
  );
  return email;
}

beforeAll(() => {
  app = initializeApp({ apiKey: 'demo-api-key', projectId: PROJECT_ID }, `integracion-${Date.now()}`);
  auth = getAuth(app);
  const db = getFirestore(app);
  connectAuthEmulator(auth, `http://${AUTH_HOST}`, { disableWarnings: true });
  const [host, puerto] = FIRESTORE_HOST.split(':');
  connectFirestoreEmulator(db, host, Number(puerto));
  repo = new FirebaseAuthRepository(auth, db);
});

afterAll(async () => {
  await deleteApp(app);
});

beforeEach(async () => {
  await repo.cerrarSesion();
  await fetch(`http://${AUTH_HOST}/emulator/v1/projects/${PROJECT_ID}/accounts`, { method: 'DELETE' });
  await fetch(`http://${FIRESTORE_HOST}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`, {
    method: 'DELETE'
  });
});

describe('FirebaseAuthRepository + Security Rules (emulador)', () => {
  it('CA1: un admin inicia sesión; el perfil se lee pese a las reglas RBAC', async () => {
    const email = await crearCuenta({ rol: 'admin' });

    const resultado = await new IniciarSesionWebUseCase(repo).ejecutar(email, CONTRASENA);

    expect(resultado.ok).toBe(true);
    if (resultado.ok) {
      expect(resultado.data).toMatchObject({ email, rol: 'admin', estado: true });
    }
    expect((await repo.obtenerSesionActual())?.rol).toBe('admin');
  });

  it('CA2: un operador es rechazado con MSG-13 y queda sin sesión', async () => {
    const email = await crearCuenta({ rol: 'operador' });

    const resultado = await new IniciarSesionWebUseCase(repo).ejecutar(email, CONTRASENA);

    expect(resultado).toEqual({ ok: false, mensaje: 'MSG-13' });
    expect(auth.currentUser).toBeNull();
  });

  it('baja lógica en Auth (gestionarUsuario) muestra MSG-03', async () => {
    const email = await crearCuenta({ rol: 'contador', deshabilitadaEnAuth: true });
    expect(await new IniciarSesionWebUseCase(repo).ejecutar(email, CONTRASENA)).toEqual({
      ok: false,
      mensaje: 'MSG-03'
    });
  });

  it('baja lógica en Firestore (estado false) muestra MSG-03 y cierra la sesión', async () => {
    const email = await crearCuenta({ rol: 'contador', estado: false });
    expect(await new IniciarSesionWebUseCase(repo).ejecutar(email, CONTRASENA)).toEqual({
      ok: false,
      mensaje: 'MSG-03'
    });
    expect(auth.currentUser).toBeNull();
  });

  it('contraseña incorrecta o usuario inexistente muestran MSG-01 opaco', async () => {
    const email = await crearCuenta({ rol: 'admin' });
    expect(await repo.iniciarSesion(email, 'Incorrecta99')).toEqual({ ok: false, mensaje: 'MSG-01' });
    expect(await repo.iniciarSesion('nadie@silmay.ec', CONTRASENA)).toEqual({ ok: false, mensaje: 'MSG-01' });
  });
});
