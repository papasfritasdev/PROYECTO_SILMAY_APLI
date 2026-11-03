import { randomUUID } from "node:crypto";
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";

/** Colecciones canónicas (docs/firebase-schema.md §2). */
export const COLECCIONES_AUDITABLES = [
  "usuarios",
  "jornadas",
  "despachos",
  "gastos",
  "maquinaria",
  "mantenimientos",
  "notificaciones",
  "conciliaciones",
  "consolidados",
  "catalogos",
  "parametros",
  "auditoria"
] as const;

export const OPERACIONES = ["read", "create", "update", "delete"] as const;
export const ORIGENES = ["web", "android"] as const;

export type ColeccionAuditable = (typeof COLECCIONES_AUDITABLES)[number];
export type Operacion = (typeof OPERACIONES)[number];
export type Origen = (typeof ORIGENES)[number];

export interface RegistrarRechazoData {
  coleccion: ColeccionAuditable;
  documentoId: string;
  operacion: Operacion;
  origen: Origen;
}

export interface RegistrarRechazoResultado {
  success: true;
  id: string;
}

const LONGITUD_MAXIMA_ID = 1500;

function incluye<T extends string>(lista: readonly T[], valor: unknown): valor is T {
  return typeof valor === "string" && (lista as readonly string[]).includes(valor);
}

function invalido(mensaje: string): functions.https.HttpsError {
  return new functions.https.HttpsError("invalid-argument", mensaje);
}

/**
 * Cloud Function HTTPS Callable: registrarRechazo (HT-07, HU-09 CA3, HU-08 CA3)
 * Región: southamerica-east1 · Roles: cualquier usuario autenticado.
 *
 * Registra en `auditoria` un rechazo PERMISSION_DENIED de las Security Rules sin abrir
 * la colección a escrituras de clientes. El autor se toma del token (`context.auth.uid`),
 * nunca del payload, para impedir registros a nombre de terceros.
 */
export const registrarRechazo = functions
  .region("southamerica-east1")
  .https.onCall(async (data: RegistrarRechazoData, context): Promise<RegistrarRechazoResultado> => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Debe estar autenticado para ejecutar esta acción."
      );
    }

    if (!incluye(COLECCIONES_AUDITABLES, data?.coleccion)) {
      throw invalido("La colección indicada no es una colección canónica del sistema.");
    }
    if (
      typeof data.documentoId !== "string" ||
      data.documentoId.trim().length === 0 ||
      data.documentoId.length > LONGITUD_MAXIMA_ID
    ) {
      throw invalido("El campo 'documentoId' es obligatorio.");
    }
    if (!incluye(OPERACIONES, data.operacion)) {
      throw invalido("La operación debe ser read, create, update o delete.");
    }
    if (!incluye(ORIGENES, data.origen)) {
      throw invalido("El origen debe ser web o android.");
    }

    const rol = context.auth.token.role;
    const id = randomUUID();
    await admin
      .firestore()
      .collection("auditoria")
      .doc(id)
      .set({
        id,
        accion: "PERMISSION_DENIED",
        coleccion: data.coleccion,
        documentoId: data.documentoId.trim(),
        usuarioId: context.auth.uid,
        detalle: {
          operacion: data.operacion,
          origen: data.origen,
          rol: typeof rol === "string" ? rol : null
        },
        fecha: FieldValue.serverTimestamp()
      });

    return { success: true, id };
  });
