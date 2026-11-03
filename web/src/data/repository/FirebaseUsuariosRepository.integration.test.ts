/**
 * Integración de FirebaseUsuariosRepository con Functions (gestionarUsuario) + Auth +
 * Firestore Emulator y Security Rules (HU-02). Ejecutar con: npm run test:integration
 */
import { deleteApp, initializeApp, type FirebaseApp } from 'firebase/app';
import { connectAuthEmulator, getAuth, signInWithEmailAndPassword, signOut, type Auth } from 'firebase/auth';
import { connectFirestoreEmulator, getFirestore, type Firestore } from 'firebase/firestore';
import { connectFunctionsEmulator, getFunctions } from 'firebase/functions';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { Rol } from '../../domain/model/Rol';
import type { DatosUsuario, Usuario } from '../../domain/model/Usuario';
import { FirebaseUsuariosRepository } from './FirebaseUsuariosRepository';

const PROJECT_ID = 'silmay-dev';
const AUTH_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST ?? '127.0.0.1:9099';
const FIRESTORE_HOST = process.env.FIRESTORE_EMULATOR_HOST ?? '127.0.0.1:8080';
const OWNER = { Authorization: 'Bearer owner', 'Content-Type': 'application/json' };
const CONTRASENA = 'Silmay2026';

let app: FirebaseApp;
let auth: Auth;
let db: Firestore;
let repo: FirebaseUsuariosRepository;

/** Crea una cuenta con claim y perfil, como lo haría el script de bootstrapping. */
async function crearCuenta(rol: Rol, cedula: string): Promise<string> {
  const email = `${rol}@silmay.ec`;
  const alta = await fetch(`http://${AUTH_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo`, {
    method: 'POST',
    headers: OWNER,
    body: JSON.stringify({ email, password: CONTRASENA })
  });
  const { localId } = (await alta.json()) as { localId: string };
  await fetch(`http://${AUTH_HOST}/identitytoolkit.googleapis.com/v1/projects/${PROJECT_ID}/accounts:update`, {
    method: 'POST',
    headers: OWNER,
    body: JSON.stringify({ localId, customAttributes: JSON.stringify({ role: rol }) })
  });
  await fetch(
    `http://${FIRESTORE_HOST}/v1/projects/${PROJECT_ID}/databases/(default)/documents/usuarios/${localId}`,
    {
      method: 'PATCH',
      headers: OWNER,
      body: JSON.stringify({
        fields: {
          uid: { stringValue: localId },
          nombres: { stringValue: `Usuario ${rol}` },
          cedula: { stringValue: cedula },
          email: { stringValue: email },
          rol: { stringValue: rol },
          zonaAsignada: { stringValue: 'administracion' },
          estado: { booleanValue: true }
        }
      })
    }
  );
  return email;
}

function alta(cambios: Partial<DatosUsuario> = {}): DatosUsuario {
  return {
    nombres: 'Juan Pérez',
    cedula: '0926687856',
    telefono: '0991234567',
    email: 'juan.perez@silmay.ec',
    password: CONTRASENA,
    rol: 'operador',
    tipoLicencia: null,
    zonaAsignada: 'cantera-banos',
    cargo: 'Operador',
    estado: true,
    ...cambios
  };
}

/** Espera la primera emisión del listado que cumpla la condición. */
function esperarUsuarios(condicion: (usuarios: readonly Usuario[]) => boolean): Promise<readonly Usuario[]> {
  return new Promise((resolver, rechazar) => {
    const cancelar = repo.observarUsuarios(
      (usuarios) => {
        if (condicion(usuarios)) {
          cancelar();
          resolver(usuarios);
        }
      },
      (mensaje) => rechazar(new Error(mensaje))
    );
  });
}

// Instancia nueva por prueba: evita que la caché local de una prueba (p. ej. como admin)
// se entregue como primer snapshot en la siguiente (p. ej. como contador).
let secuencia = 0;

function crearInstancia(): void {
  secuencia += 1;
  app = initializeApp({ apiKey: 'demo-api-key', projectId: PROJECT_ID }, `usuarios-${secuencia}`);
  auth = getAuth(app);
  db = getFirestore(app);
  const functions = getFunctions(app, 'southamerica-east1');
  connectAuthEmulator(auth, `http://${AUTH_HOST}`, { disableWarnings: true });
  const [host, puerto] = FIRESTORE_HOST.split(':');
  connectFirestoreEmulator(db, host, Number(puerto));
  connectFunctionsEmulator(functions, '127.0.0.1', 5001);
  repo = new FirebaseUsuariosRepository(db, functions);
}

afterEach(async () => {
  await signOut(auth);
  await deleteApp(app);
});

beforeEach(async () => {
  crearInstancia();
  await fetch(`http://${AUTH_HOST}/emulator/v1/projects/${PROJECT_ID}/accounts`, { method: 'DELETE' });
  await fetch(`http://${FIRESTORE_HOST}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`, {
    method: 'DELETE'
  });
});

describe('FirebaseUsuariosRepository + gestionarUsuario (emuladores)', () => {
  it('CA1: el admin crea un usuario y aparece en el listado en tiempo real', async () => {
    await signInWithEmailAndPassword(auth, await crearCuenta('admin', '1710034065'), CONTRASENA);

    const resultado = await repo.guardarUsuario(alta());

    expect(resultado.ok).toBe(true);
    const usuarios = await esperarUsuarios((lista) => lista.some((u) => u.email === 'juan.perez@silmay.ec'));
    expect(usuarios.find((u) => u.email === 'juan.perez@silmay.ec')).toMatchObject({
      rol: 'operador',
      estado: true,
      telefono: '0991234567'
    });
  });

  it('CA2: un correo duplicado retorna MSG-17', async () => {
    await signInWithEmailAndPassword(auth, await crearCuenta('admin', '1710034065'), CONTRASENA);
    await repo.guardarUsuario(alta());

    expect(await repo.guardarUsuario(alta({ cedula: '0922879580' }))).toEqual({ ok: false, mensaje: 'MSG-17' });
  });

  it('CA3: la baja lógica se refleja en el listado', async () => {
    await signInWithEmailAndPassword(auth, await crearCuenta('admin', '1710034065'), CONTRASENA);
    const creado = await repo.guardarUsuario(alta());
    if (!creado.ok) throw new Error('alta fallida');

    const { password: _sinCambio, ...sinPassword } = alta();
    expect((await repo.guardarUsuario({ ...sinPassword, uid: creado.data, estado: false })).ok).toBe(true);

    const usuarios = await esperarUsuarios((lista) => lista.some((u) => u.uid === creado.data && !u.estado));
    expect(usuarios.find((u) => u.uid === creado.data)?.estado).toBe(false);
  });

  it('un contador no puede gestionar usuarios: MSG-13', async () => {
    await signInWithEmailAndPassword(auth, await crearCuenta('contador', '1710034065'), CONTRASENA);
    expect(await repo.guardarUsuario(alta())).toEqual({ ok: false, mensaje: 'MSG-13' });
  });

  it('las Security Rules impiden al contador listar usuarios: MSG-13', async () => {
    await signInWithEmailAndPassword(auth, await crearCuenta('contador', '1710034065'), CONTRASENA);
    await expect(esperarUsuarios(() => true)).rejects.toThrow('MSG-13');
  });
});
