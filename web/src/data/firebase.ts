import { initializeApp } from 'firebase/app';
import { connectAuthEmulator, getAuth } from 'firebase/auth';
import { connectFirestoreEmulator, getFirestore } from 'firebase/firestore';
import { connectFunctionsEmulator, getFunctions } from 'firebase/functions';

/**
 * Inicialización única del Firebase Web SDK (docs/setup-environment.md §3.3).
 *
 * Decisión del equipo (Sprint 2): desarrollo solo con Firebase Emulator Suite. Sin variables
 * de entorno se usan valores de demostración que el emulador acepta; para la nube deben
 * definirse las variables VITE_FIREBASE_* (nunca versionadas).
 */
const env = import.meta.env;

const config = {
  apiKey: env.VITE_FIREBASE_API_KEY ?? 'demo-api-key-emulador',
  authDomain: env.VITE_FIREBASE_AUTH_DOMAIN ?? 'silmay-dev.firebaseapp.com',
  projectId: env.VITE_FIREBASE_PROJECT_ID ?? 'silmay-dev',
  storageBucket: env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: env.VITE_FIREBASE_APP_ID
};

export const usaEmuladores =
  env.VITE_USE_FIREBASE_EMULATORS !== undefined
    ? env.VITE_USE_FIREBASE_EMULATORS === 'true'
    : env.DEV;

export const REGION_FUNCTIONS = 'southamerica-east1';

const app = initializeApp(config);

export const auth = getAuth(app);
export const db = getFirestore(app);
export const functions = getFunctions(app, REGION_FUNCTIONS);

if (usaEmuladores) {
  connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
  connectFirestoreEmulator(db, '127.0.0.1', 8080);
  connectFunctionsEmulator(functions, '127.0.0.1', 5001);
}
