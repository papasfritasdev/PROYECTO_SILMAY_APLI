/**
 * Utilidades compartidas por las pruebas de integración web (Firebase Emulator Suite).
 * Usan la API REST de los emuladores con privilegio `owner` (omite Security Rules),
 * igual que lo harían gestionarUsuario o los scripts de bootstrapping.
 */
import { deleteApp, initializeApp, type FirebaseApp } from 'firebase/app';
import { connectAuthEmulator, getAuth, signOut, type Auth } from 'firebase/auth';
import { connectFirestoreEmulator, getFirestore, type Firestore } from 'firebase/firestore';
import { connectFunctionsEmulator, getFunctions, type Functions } from 'firebase/functions';
import type { Rol } from '../domain/model/Rol';

export const PROJECT_ID = 'silmay-dev';
export const CONTRASENA_PRUEBA = 'Silmay2026';
const AUTH_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST ?? '127.0.0.1:9099';
const FIRESTORE_HOST = process.env.FIRESTORE_EMULATOR_HOST ?? '127.0.0.1:8080';
const OWNER = { Authorization: 'Bearer owner', 'Content-Type': 'application/json' };
const DOCUMENTOS = `http://${FIRESTORE_HOST}/v1/projects/${PROJECT_ID}/databases/(default)/documents`;

export interface InstanciaFirebase {
  readonly app: FirebaseApp;
  readonly auth: Auth;
  readonly db: Firestore;
  readonly functions: Functions;
  cerrar(): Promise<void>;
}

let secuencia = 0;

/** Instancia aislada por prueba: evita que la caché local de una prueba contamine la siguiente. */
export function crearInstancia(): InstanciaFirebase {
  secuencia += 1;
  const app = initializeApp({ apiKey: 'demo-api-key', projectId: PROJECT_ID }, `integracion-${secuencia}`);
  const auth = getAuth(app);
  const db = getFirestore(app);
  const functions = getFunctions(app, 'southamerica-east1');
  connectAuthEmulator(auth, `http://${AUTH_HOST}`, { disableWarnings: true });
  const [host, puerto] = FIRESTORE_HOST.split(':');
  connectFirestoreEmulator(db, host, Number(puerto));
  connectFunctionsEmulator(functions, '127.0.0.1', 5001);
  return {
    app,
    auth,
    db,
    functions,
    async cerrar() {
      await signOut(auth);
      await deleteApp(app);
    }
  };
}

export async function limpiarEmuladores(): Promise<void> {
  await fetch(`http://${AUTH_HOST}/emulator/v1/projects/${PROJECT_ID}/accounts`, { method: 'DELETE' });
  await fetch(`http://${FIRESTORE_HOST}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`, {
    method: 'DELETE'
  });
}

export interface OpcionesCuenta {
  readonly rol: Rol;
  readonly email?: string;
  readonly cedula?: string;
  readonly estado?: boolean;
  readonly deshabilitadaEnAuth?: boolean;
}

/** Crea una cuenta con Custom Claim y su perfil en `usuarios`. Retorna el uid y el correo. */
export async function crearCuenta({
  rol,
  email = `${rol}@silmay.ec`,
  cedula = '1710034065',
  estado = true,
  deshabilitadaEnAuth = false
}: OpcionesCuenta): Promise<{ uid: string; email: string }> {
  const alta = await fetch(`http://${AUTH_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo`, {
    method: 'POST',
    headers: OWNER,
    body: JSON.stringify({ email, password: CONTRASENA_PRUEBA })
  });
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

  await fetch(`${DOCUMENTOS}/usuarios/${localId}`, {
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
        estado: { booleanValue: estado }
      }
    })
  });
  return { uid: localId, email };
}

interface DocumentoRest {
  readonly fields?: Readonly<Record<string, { readonly stringValue?: string }>>;
}

/** Lee una colección como owner (p. ej. `auditoria`, cerrada a clientes). Campos string. */
export async function leerColeccionComoOwner(coleccion: string): Promise<Record<string, string>[]> {
  const respuesta = await fetch(`${DOCUMENTOS}/${coleccion}`, { headers: OWNER });
  const { documents = [] } = (await respuesta.json()) as { documents?: DocumentoRest[] };
  return documents.map((d) =>
    Object.fromEntries(
      Object.entries(d.fields ?? {})
        .filter(([, v]) => typeof v.stringValue === 'string')
        .map(([k, v]) => [k, v.stringValue as string])
    )
  );
}
