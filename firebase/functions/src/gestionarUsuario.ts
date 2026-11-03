import { randomUUID } from "node:crypto";
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import {
  esRolValido,
  esCedulaEcuatorianaValida,
  esContrasenaValida,
  esEmailValido,
  esTelefonoEcuatorianoValido,
  LONGITUD_MINIMA_CONTRASENA,
  RolUsuario
} from "./utils/validaciones";

export interface GestionarUsuarioData {
  uid?: string;
  nombres: string;
  cedula: string;
  telefono: string;
  email: string;
  password?: string;
  rol: RolUsuario;
  tipoLicencia?: string | null;
  zonaAsignada: string;
  cargo: string;
  estado?: boolean;
}

/** Campo duplicado informado en `details` para que el cliente muestre MSG-17 (HU-02 CA2). */
export type CampoDuplicado = "email" | "cedula";

export interface GestionarUsuarioResultado {
  success: true;
  uid: string;
  message: string;
}

/** Documento canónico de `usuarios` (docs/firebase-schema.md §2.1). */
interface UsuarioDoc {
  uid: string;
  nombres: string;
  cedula: string;
  telefono: string;
  email: string;
  rol: RolUsuario;
  tipoLicencia: string | null;
  zonaAsignada: string;
  cargo: string;
  estado: boolean;
}

const COLECCION_USUARIOS = "usuarios";
const COLECCION_AUDITORIA = "auditoria";

function invalido(mensaje: string): functions.https.HttpsError {
  return new functions.https.HttpsError("invalid-argument", mensaje);
}

function textoObligatorio(valor: unknown, campo: string): string {
  if (typeof valor !== "string" || valor.trim().length === 0) {
    throw invalido(`El campo '${campo}' es obligatorio.`);
  }
  return valor.trim();
}

/** Valida y normaliza el payload. Lanza HttpsError('invalid-argument') ante cualquier fallo. */
function validarPayload(data: GestionarUsuarioData, esActualizacion: boolean): Omit<UsuarioDoc, "uid"> {
  const nombres = textoObligatorio(data.nombres, "nombres");

  if (!esCedulaEcuatorianaValida(data.cedula)) {
    throw invalido("La cédula ingresada no es una cédula ecuatoriana válida de 10 dígitos.");
  }

  if (!esEmailValido(data.email)) {
    throw invalido("El formato del correo electrónico institucional es inválido.");
  }

  if (!esTelefonoEcuatorianoValido(data.telefono)) {
    throw invalido("El teléfono debe ser un número ecuatoriano válido (celular o fijo).");
  }

  if (!esRolValido(data.rol)) {
    throw invalido(
      "El rol proporcionado no es válido. Roles permitidos: operador, supervisor, contador, admin."
    );
  }

  const zonaAsignada = textoObligatorio(data.zonaAsignada, "zonaAsignada");
  const cargo = textoObligatorio(data.cargo, "cargo");

  const contrasenaObligatoria = !esActualizacion;
  const contrasenaInformada = data.password !== undefined && data.password !== "";
  if ((contrasenaObligatoria || contrasenaInformada) && !esContrasenaValida(data.password)) {
    throw invalido(
      `La contraseña debe tener al menos ${LONGITUD_MINIMA_CONTRASENA} caracteres e incluir letras y números.`
    );
  }

  if (data.estado !== undefined && typeof data.estado !== "boolean") {
    throw invalido("El campo 'estado' debe ser booleano.");
  }

  const tipoLicencia =
    typeof data.tipoLicencia === "string" && data.tipoLicencia.trim().length > 0
      ? data.tipoLicencia.trim()
      : null;

  return {
    nombres,
    cedula: data.cedula.trim(),
    telefono: data.telefono.replace(/[\s-]/g, ""),
    email: data.email.trim().toLowerCase(),
    rol: data.rol,
    tipoLicencia,
    zonaAsignada,
    cargo,
    estado: data.estado ?? true
  };
}

/** HU-02 CA2: unicidad de cédula y correo en `usuarios`, excluyendo al propio usuario editado. */
async function verificarUnicidad(
  db: admin.firestore.Firestore,
  campo: CampoDuplicado,
  valor: string,
  uidPropio: string | undefined
): Promise<void> {
  const resultado = await db
    .collection(COLECCION_USUARIOS)
    .where(campo, "==", valor)
    .limit(2)
    .get();

  const duplicado = resultado.docs.some((d) => d.id !== uidPropio);
  if (duplicado) {
    throw new functions.https.HttpsError(
      "already-exists",
      campo === "email"
        ? "Ya existe un usuario registrado con ese correo electrónico."
        : "Ya existe un usuario registrado con esa cédula.",
      { campo }
    );
  }
}

/** Entrada append-only de auditoría con ID UUID v4 (prohibido add(), CLAUDE.md). */
function registrarAuditoria(
  batch: admin.firestore.WriteBatch,
  db: admin.firestore.Firestore,
  accion: "CREATE" | "UPDATE",
  documentoId: string,
  usuarioId: string,
  detalle: Record<string, unknown>
): void {
  const id = randomUUID();
  batch.set(db.collection(COLECCION_AUDITORIA).doc(id), {
    id,
    accion,
    coleccion: COLECCION_USUARIOS,
    documentoId,
    usuarioId,
    detalle,
    fecha: admin.firestore.FieldValue.serverTimestamp()
  });
}

function codigoDeError(error: unknown): string | undefined {
  if (typeof error === "object" && error !== null && "code" in error) {
    const code = (error as { code: unknown }).code;
    return typeof code === "string" ? code : undefined;
  }
  return undefined;
}

async function crearUsuario(
  db: admin.firestore.Firestore,
  auth: admin.auth.Auth,
  perfil: Omit<UsuarioDoc, "uid">,
  password: string,
  autorUid: string
): Promise<GestionarUsuarioResultado> {
  const nuevo = await auth.createUser({
    email: perfil.email,
    password,
    displayName: perfil.nombres,
    disabled: !perfil.estado
  });

  try {
    await auth.setCustomUserClaims(nuevo.uid, { role: perfil.rol });

    const usuarioDoc: UsuarioDoc = { uid: nuevo.uid, ...perfil };
    const batch = db.batch();
    batch.set(db.collection(COLECCION_USUARIOS).doc(nuevo.uid), usuarioDoc);
    registrarAuditoria(batch, db, "CREATE", nuevo.uid, autorUid, {
      email: perfil.email,
      rol: perfil.rol,
      zonaAsignada: perfil.zonaAsignada,
      estado: perfil.estado
    });
    await batch.commit();
  } catch (error: unknown) {
    // Compensación: sin perfil en Firestore la cuenta de Auth quedaría huérfana e inutilizable.
    await auth.deleteUser(nuevo.uid);
    throw error;
  }

  return {
    success: true,
    uid: nuevo.uid,
    message: "Usuario creado exitosamente con Custom Claims asignados."
  };
}

async function actualizarUsuario(
  db: admin.firestore.Firestore,
  auth: admin.auth.Auth,
  uid: string,
  perfil: Omit<UsuarioDoc, "uid">,
  password: string | undefined,
  autorUid: string
): Promise<GestionarUsuarioResultado> {
  const ref = db.collection(COLECCION_USUARIOS).doc(uid);
  const anterior = await ref.get();
  if (!anterior.exists) {
    throw new functions.https.HttpsError("not-found", "El usuario a actualizar no existe.");
  }
  const previo = anterior.data() as UsuarioDoc;

  const cambios: admin.auth.UpdateRequest = {
    email: perfil.email,
    displayName: perfil.nombres,
    disabled: !perfil.estado
  };
  if (password) {
    cambios.password = password;
  }

  await auth.updateUser(uid, cambios);
  await auth.setCustomUserClaims(uid, { role: perfil.rol });

  const bajaLogica = previo.estado === true && perfil.estado === false;
  const cambioDeRol = previo.rol !== perfil.rol;

  // HU-02 CA3: la baja y el cambio de rol surten efecto inmediato en todas las sesiones.
  if (bajaLogica || cambioDeRol) {
    await auth.revokeRefreshTokens(uid);
  }

  const usuarioDoc: UsuarioDoc = { uid, ...perfil };
  const batch = db.batch();
  batch.set(ref, usuarioDoc, { merge: true });
  registrarAuditoria(batch, db, "UPDATE", uid, autorUid, {
    antes: { rol: previo.rol, estado: previo.estado, zonaAsignada: previo.zonaAsignada },
    despues: { rol: perfil.rol, estado: perfil.estado, zonaAsignada: perfil.zonaAsignada },
    bajaLogica,
    reactivacion: previo.estado === false && perfil.estado === true,
    cambioDeRol,
    cambioDeContrasena: Boolean(password)
  });
  await batch.commit();

  return { success: true, uid, message: "Usuario actualizado exitosamente." };
}

/**
 * Cloud Function HTTPS Callable: gestionarUsuario (HT-04, HU-02)
 * Región: southamerica-east1 (ADR-0009) · Rol autorizado: admin (RF-05, RNF-08)
 *
 * Errores para el cliente web:
 *  - unauthenticated / permission-denied: RBAC.
 *  - invalid-argument: validación de payload.
 *  - already-exists + details.campo ('email' | 'cedula'): mostrar MSG-17.
 *  - failed-precondition: el admin intenta darse de baja o quitarse el rol admin.
 *  - not-found: actualización de un uid inexistente.
 */
export const gestionarUsuario = functions
  .region("southamerica-east1")
  .https.onCall(async (data: GestionarUsuarioData, context): Promise<GestionarUsuarioResultado> => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Debe estar autenticado para ejecutar esta acción."
      );
    }

    if (context.auth.token.role !== "admin") {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Permiso denegado: Se requiere rol de Administrador."
      );
    }

    const autorUid = context.auth.uid;
    const uidObjetivo = typeof data.uid === "string" && data.uid.length > 0 ? data.uid : undefined;
    const esActualizacion = uidObjetivo !== undefined;
    const perfil = validarPayload(data, esActualizacion);

    // Evita que el sistema quede sin administrador por un error del propio admin.
    if (uidObjetivo === autorUid && (!perfil.estado || perfil.rol !== "admin")) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Un administrador no puede darse de baja ni retirarse su propio rol."
      );
    }

    const db = admin.firestore();
    const auth = admin.auth();

    await verificarUnicidad(db, "cedula", perfil.cedula, uidObjetivo);
    await verificarUnicidad(db, "email", perfil.email, uidObjetivo);

    try {
      if (uidObjetivo) {
        return await actualizarUsuario(db, auth, uidObjetivo, perfil, data.password || undefined, autorUid);
      }
      return await crearUsuario(db, auth, perfil, data.password as string, autorUid);
    } catch (error: unknown) {
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }
      const codigo = codigoDeError(error);
      if (codigo === "auth/email-already-exists") {
        throw new functions.https.HttpsError(
          "already-exists",
          "Ya existe una cuenta registrada con ese correo electrónico.",
          { campo: "email" satisfies CampoDuplicado }
        );
      }
      if (codigo === "auth/user-not-found") {
        throw new functions.https.HttpsError("not-found", "El usuario a actualizar no existe.");
      }
      const mensaje = error instanceof Error ? error.message : "Error desconocido";
      throw new functions.https.HttpsError(
        "internal",
        `Error al procesar la gestión de usuario: ${mensaje}`
      );
    }
  });
