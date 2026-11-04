import * as admin from "firebase-admin";

// Inicialización única de Firebase Admin SDK
if (!admin.apps.length) {
  admin.initializeApp();
}

// Exportación de microservicios serverless
export { gestionarUsuario } from "./gestionarUsuario"; // HT-04, HU-02
export { registrarRechazo } from "./registrarRechazo"; // HU-09 CA3, HT-07
export { respaldoMensual } from "./respaldoMensual"; // HT-12, RNF-16
