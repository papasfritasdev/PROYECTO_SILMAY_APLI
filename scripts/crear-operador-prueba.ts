/**
 * Script para crear usuario Operador de prueba en el Sistema SILMAY
 * docs/specs/sprint-01-specs.md (HU-01a / Paso 23)
 *
 * Uso:
 *   npx ts-node scripts/crear-operador-prueba.ts --local
 */

import * as admin from 'firebase-admin';
import * as dotenv from 'dotenv';

dotenv.config();

const OPERADOR_CONFIG = {
  email: 'operador@silmay.ec',
  password: 'Silmay2026!Operador',
  nombres: 'Carlos Segundo Operador',
  cedula: '0922879580', // Cédula ecuatoriana válida canónica (Guayas)
  telefono: '+593988888888',
  cargo: 'Operador de Maquinaria Pesada',
  zonaAsignada: 'cantera-banos',
  tipoLicencia: 'Tipo G',
  rol: 'operador' as const
};

async function main() {
  const isLocal = process.argv.includes('--local') || !process.env.GOOGLE_APPLICATION_CREDENTIALS;

  if (isLocal) {
    process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080';
    process.env.FIREBASE_AUTH_EMULATOR_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST || '127.0.0.1:9099';
    console.log(' Conectando a Firebase Emulator Suite:');
    console.log(`   - Auth Emulator:      ${process.env.FIREBASE_AUTH_EMULATOR_HOST}`);
    console.log(`   - Firestore Emulator: ${process.env.FIRESTORE_EMULATOR_HOST}`);
  }

  if (!admin.apps.length) {
    admin.initializeApp({
      projectId: process.env.FIREBASE_PROJECT_ID || 'silmay-dev'
    });
  }

  const auth = admin.auth();
  const db = admin.firestore();

  console.log(`\n Verificando cuenta de Operador: ${OPERADOR_CONFIG.email}`);

  let uid: string;

  try {
    const existingUser = await auth.getUserByEmail(OPERADOR_CONFIG.email);
    uid = existingUser.uid;
    console.log(` Usuario existente en Auth con UID: ${uid}`);

    await auth.updateUser(uid, {
      password: OPERADOR_CONFIG.password,
      displayName: OPERADOR_CONFIG.nombres,
      disabled: false
    });
  } catch (error: unknown) {
    const err = error as { code?: string };
    if (err.code === 'auth/user-not-found') {
      console.log(' Creando nuevo usuario Operador en Firebase Auth...');
      const newUser = await auth.createUser({
        email: OPERADOR_CONFIG.email,
        password: OPERADOR_CONFIG.password,
        displayName: OPERADOR_CONFIG.nombres,
        disabled: false
      });
      uid = newUser.uid;
      console.log(` Usuario creado con UID: ${uid}`);
    } else {
      throw error;
    }
  }

  // 1. Inyectar Custom Claim autoritativo role: 'operador'
  console.log(` Inyectando Custom Claim: { role: '${OPERADOR_CONFIG.rol}' }...`);
  await auth.setCustomUserClaims(uid, { role: OPERADOR_CONFIG.rol });

  // 2. Persistir en colección 'usuarios'
  console.log(' Guardando documento de perfil en Firestore (colección "usuarios")...');
  const usuarioDoc = {
    uid,
    nombres: OPERADOR_CONFIG.nombres,
    cedula: OPERADOR_CONFIG.cedula,
    telefono: OPERADOR_CONFIG.telefono,
    email: OPERADOR_CONFIG.email.toLowerCase(),
    rol: OPERADOR_CONFIG.rol,
    tipoLicencia: OPERADOR_CONFIG.tipoLicencia,
    zonaAsignada: OPERADOR_CONFIG.zonaAsignada,
    cargo: OPERADOR_CONFIG.cargo,
    estado: true
  };

  await db.collection('usuarios').doc(uid).set(usuarioDoc, { merge: true });

  console.log('\n ==============================================================');
  console.log('   OPERADOR DE PRUEBA CREADO EXITOSAMENTE');
  console.log(' ==============================================================');
  console.log(`   UID:          ${uid}`);
  console.log(`   Correo:       ${OPERADOR_CONFIG.email}`);
  console.log(`   Contraseña:   ${OPERADOR_CONFIG.password}`);
  console.log(`   Custom Claim: role='${OPERADOR_CONFIG.rol}'`);
  console.log(`   Colección:    usuarios/${uid}`);
  console.log(' ==============================================================\n');
}

main().catch((error) => {
  console.error('\n❌ Error al crear el Operador de prueba:', error);
  process.exit(1);
});
