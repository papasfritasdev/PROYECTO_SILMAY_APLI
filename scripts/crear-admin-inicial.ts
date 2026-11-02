/**
 * Script de bootstrapping para el Sistema SILMAY
 * docs/specs/sprint-01-specs.md (HT-04) y docs/firebase-schema.md
 *
 * Uso:
 *   Local (Emuladores): npm run crear-admin:local
 *   Producción (GCP):   npm run crear-admin:prod
 */

import * as admin from 'firebase-admin';
import * as dotenv from 'dotenv';

dotenv.config();

// Configuración de credenciales de Administrador por defecto
const ADMIN_CONFIG = {
  email: process.env.ADMIN_EMAIL || 'admin@silmay.ec',
  password: process.env.ADMIN_PASSWORD || 'Silmay2026!Admin',
  nombres: 'Administrador General SILMAY',
  cedula: '1710034065', // Cédula ecuatoriana válida canónica
  telefono: '+593999999999',
  cargo: 'Administrador del Sistema',
  zonaAsignada: 'administracion',
  rol: 'admin' as const
};

async function main() {
  const isLocal = process.argv.includes('--local') || !process.env.GOOGLE_APPLICATION_CREDENTIALS;

  if (isLocal) {
    process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080';
    process.env.FIREBASE_AUTH_EMULATOR_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST || '127.0.0.1:9099';
    console.log(' Conectando a Firebase Emulator Suite:');
    console.log(`   - Auth Emulator:      ${process.env.FIREBASE_AUTH_EMULATOR_HOST}`);
    console.log(`   - Firestore Emulator: ${process.env.FIRESTORE_EMULATOR_HOST}`);
  } else {
    console.log(' Conectando a Firebase Cloud (Producción/Staging)...');
  }

  // Inicializar Firebase Admin
  if (!admin.apps.length) {
    admin.initializeApp({
      projectId: process.env.FIREBASE_PROJECT_ID || 'silmay-dev'
    });
  }

  const auth = admin.auth();
  const db = admin.firestore();

  console.log(`\n Verificando cuenta de Administrador: ${ADMIN_CONFIG.email}`);

  let uid: string;

  try {
    const existingUser = await auth.getUserByEmail(ADMIN_CONFIG.email);
    uid = existingUser.uid;
    console.log(` Usuario existente encontrado en Auth con UID: ${uid}`);

    // Asegurar contraseña y estado
    await auth.updateUser(uid, {
      password: ADMIN_CONFIG.password,
      displayName: ADMIN_CONFIG.nombres,
      disabled: false
    });
  } catch (error: unknown) {
    const err = error as { code?: string };
    if (err.code === 'auth/user-not-found') {
      console.log(' Creando nuevo usuario Administrador en Firebase Auth...');
      const newUser = await auth.createUser({
        email: ADMIN_CONFIG.email,
        password: ADMIN_CONFIG.password,
        displayName: ADMIN_CONFIG.nombres,
        disabled: false
      });
      uid = newUser.uid;
      console.log(` Usuario creado con UID: ${uid}`);
    } else {
      throw error;
    }
  }

  // 1. Inyectar Custom Claim autoritativo role: 'admin'
  console.log(` Inyectando Custom Claim: { role: '${ADMIN_CONFIG.rol}' }...`);
  await auth.setCustomUserClaims(uid, { role: ADMIN_CONFIG.rol });

  // 2. Persistir perfil canónico en colección 'usuarios'
  console.log(' Guardando documento de perfil en Firestore (colección "usuarios")...');
  const usuarioDoc = {
    uid,
    nombres: ADMIN_CONFIG.nombres,
    cedula: ADMIN_CONFIG.cedula,
    telefono: ADMIN_CONFIG.telefono,
    email: ADMIN_CONFIG.email.toLowerCase(),
    rol: ADMIN_CONFIG.rol,
    tipoLicencia: null,
    zonaAsignada: ADMIN_CONFIG.zonaAsignada,
    cargo: ADMIN_CONFIG.cargo,
    estado: true
  };

  await db.collection('usuarios').doc(uid).set(usuarioDoc, { merge: true });

  console.log('\n ==============================================================');
  console.log('   ADMINISTRADOR INICIAL CONFIGURADO EXITOSAMENTE');
  console.log(' ==============================================================');
  console.log(`   UID:          ${uid}`);
  console.log(`   Correo:       ${ADMIN_CONFIG.email}`);
  console.log(`   Contraseña:   ${ADMIN_CONFIG.password}`);
  console.log(`   Custom Claim: role='${ADMIN_CONFIG.rol}'`);
  console.log(`   Colección:    usuarios/${uid}`);
  console.log(' ==============================================================\n');
}

main().catch((error) => {
  console.error('\n❌ Error al crear el Administrador inicial:', error);
  process.exit(1);
});
