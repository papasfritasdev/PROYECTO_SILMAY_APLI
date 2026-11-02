import * as admin from "firebase-admin";

// Inicialización única de Firebase Admin SDK
if (!admin.apps.length) {
  admin.initializeApp();
}

// Exportación de microservicios serverless (HT-04)
export { gestionarUsuario } from "./gestionarUsuario";
