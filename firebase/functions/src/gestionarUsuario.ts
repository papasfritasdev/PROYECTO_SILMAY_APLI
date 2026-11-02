import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import {
  esRolValido,
  esCedulaEcuatorianaValida,
  esEmailValido,
  RolUsuario
} from "./utils/validaciones";

export interface GestionarUsuarioData {
  uid?: string;
  nombres: string;
  cedula: string;
  telefono?: string | null;
  email: string;
  password?: string;
  rol: RolUsuario;
  tipoLicencia?: string | null;
  zonaAsignada: string;
  cargo: string;
  estado?: boolean;
}

/**
 * Cloud Function HTTPS Callable: gestionarUsuario
 * Región: southamerica-east1 (ADR-0009)
 * Roles autorizados: admin (RF-05, RNF-08)
 */
export const gestionarUsuario = functions
  .region("southamerica-east1")
  .https.onCall(async (data: GestionarUsuarioData, context) => {
    // 1. Verificación de Autenticación y Autorización RBAC
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Debe estar autenticado para ejecutar esta acción."
      );
    }

    const callerRole = context.auth.token.role;
    if (callerRole !== "admin") {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Permiso denegado: Se requiere rol de Administrador."
      );
    }

    // 2. Validaciones de Payload
    if (!data.nombres || typeof data.nombres !== "string" || data.nombres.trim().length === 0) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "El campo 'nombres' es obligatorio."
      );
    }

    if (!esCedulaEcuatorianaValida(data.cedula)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "La cédula ingresada no es una cédula ecuatoriana válida de 10 dígitos."
      );
    }

    if (!esEmailValido(data.email)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "El formato del correo electrónico institucional es inválido."
      );
    }

    if (!esRolValido(data.rol)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "El rol proporcionado no es válido. Roles permitidos: operador, supervisor, contador, admin."
      );
    }

    if (!data.zonaAsignada || typeof data.zonaAsignada !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "El campo 'zonaAsignada' es obligatorio."
      );
    }

    if (!data.cargo || typeof data.cargo !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "El campo 'cargo' es obligatorio."
      );
    }

    const db = admin.firestore();
    const auth = admin.auth();
    const esActualizacion = Boolean(data.uid);
    let targetUid = data.uid;

    // 3. Validación de Unicidad de Cédula en Firestore
    const cedulaQuery = await db
      .collection("usuarios")
      .where("cedula", "==", data.cedula.trim())
      .limit(1)
      .get();

    if (!cedulaQuery.empty) {
      const existingDoc = cedulaQuery.docs[0];
      if (!esActualizacion || existingDoc.id !== targetUid) {
        throw new functions.https.HttpsError(
          "already-exists",
          "Ya existe un usuario registrado con esa cédula."
        );
      }
    }

    // 4. Ejecución en Firebase Auth y Firestore
    try {
      if (esActualizacion && targetUid) {
        // --- Actualización de Usuario Existente ---
        const userUpdates: admin.auth.UpdateRequest = {
          email: data.email.trim().toLowerCase(),
          displayName: data.nombres.trim()
        };

        if (typeof data.estado === "boolean") {
          userUpdates.disabled = !data.estado;
        }

        if (data.password && data.password.trim().length >= 6) {
          userUpdates.password = data.password.trim();
        }

        await auth.updateUser(targetUid, userUpdates);
        await auth.setCustomUserClaims(targetUid, { role: data.rol });

        const usuarioDoc = {
          uid: targetUid,
          nombres: data.nombres.trim(),
          cedula: data.cedula.trim(),
          telefono: data.telefono ? data.telefono.trim() : null,
          email: data.email.trim().toLowerCase(),
          rol: data.rol,
          tipoLicencia: data.tipoLicencia ? data.tipoLicencia.trim() : null,
          zonaAsignada: data.zonaAsignada.trim(),
          cargo: data.cargo.trim(),
          estado: data.estado !== undefined ? data.estado : true
        };

        await db.collection("usuarios").doc(targetUid).set(usuarioDoc, { merge: true });

        // Registrar auditoría
        await db.collection("auditoria").add({
          accion: "UPDATE",
          coleccion: "usuarios",
          documentoId: targetUid,
          usuarioId: context.auth.uid,
          detalle: {
            rol: data.rol,
            estado: usuarioDoc.estado,
            zonaAsignada: usuarioDoc.zonaAsignada
          },
          fecha: admin.firestore.FieldValue.serverTimestamp()
        });

        return {
          success: true,
          uid: targetUid,
          message: "Usuario actualizado exitosamente."
        };
      } else {
        // --- Creación de Nuevo Usuario ---
        if (!data.password || data.password.trim().length < 6) {
          throw new functions.https.HttpsError(
            "invalid-argument",
            "La contraseña inicial debe tener al menos 6 caracteres."
          );
        }

        const newUser = await auth.createUser({
          email: data.email.trim().toLowerCase(),
          password: data.password.trim(),
          displayName: data.nombres.trim(),
          disabled: data.estado === false
        });

        targetUid = newUser.uid;

        // Inyectar Custom Claim autoritativo
        await auth.setCustomUserClaims(targetUid, { role: data.rol });

        const usuarioDoc = {
          uid: targetUid,
          nombres: data.nombres.trim(),
          cedula: data.cedula.trim(),
          telefono: data.telefono ? data.telefono.trim() : null,
          email: data.email.trim().toLowerCase(),
          rol: data.rol,
          tipoLicencia: data.tipoLicencia ? data.tipoLicencia.trim() : null,
          zonaAsignada: data.zonaAsignada.trim(),
          cargo: data.cargo.trim(),
          estado: data.estado !== undefined ? data.estado : true
        };

        // Persistir en Firestore (Idempotente con document(uid).set)
        await db.collection("usuarios").doc(targetUid).set(usuarioDoc);

        // Registrar auditoría
        await db.collection("auditoria").add({
          accion: "CREATE",
          coleccion: "usuarios",
          documentoId: targetUid,
          usuarioId: context.auth.uid,
          detalle: {
            email: usuarioDoc.email,
            rol: usuarioDoc.rol,
            zonaAsignada: usuarioDoc.zonaAsignada
          },
          fecha: admin.firestore.FieldValue.serverTimestamp()
        });

        return {
          success: true,
          uid: targetUid,
          message: "Usuario creado exitosamente con Custom Claims asignados."
        };
      }
    } catch (error: unknown) {
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }
      const err = error as { code?: string; message?: string };
      if (err.code === "auth/email-already-exists") {
        throw new functions.https.HttpsError(
          "already-exists",
          "Ya existe una cuenta registrada con ese correo electrónico."
        );
      }
      throw new functions.https.HttpsError(
        "internal",
        `Error al procesar la gestión de usuario: ${err.message || "Error desconocido"}`
      );
    }
  });
