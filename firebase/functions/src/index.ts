import * as admin from "firebase-admin";

// Inicialización única de Firebase Admin SDK
if (!admin.apps.length) {
  admin.initializeApp();
}

// En el Paso 9 exportaremos aquí la función callable gestionarUsuario
