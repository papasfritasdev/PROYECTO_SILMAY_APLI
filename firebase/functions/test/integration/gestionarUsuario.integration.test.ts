/**
 * Pruebas de integración de gestionarUsuario contra Auth + Firestore Emulator (HU-02).
 * Ejecutar con: npm run test:integration
 */
import { afterAll, beforeAll, beforeEach, describe, expect, it } from "vitest";
import * as admin from "firebase-admin";
import fft from "firebase-functions-test";
import { gestionarUsuario, GestionarUsuarioData } from "../../src/gestionarUsuario";

const PROJECT_ID = process.env.GCLOUD_PROJECT || "silmay-dev";
const testEnv = fft({ projectId: PROJECT_ID });
const wrapped = testEnv.wrap(gestionarUsuario);

const ADMIN_UID = "admin-integracion";
const contextoAdmin = { auth: { uid: ADMIN_UID, token: { role: "admin" } } };

function base(overrides: Partial<GestionarUsuarioData> = {}): GestionarUsuarioData {
  return {
    nombres: "Juan Pérez",
    cedula: "1710034065",
    telefono: "0991234567",
    email: "juan.perez@silmay.ec",
    password: "Silmay2026",
    rol: "operador",
    tipoLicencia: "Tipo G",
    zonaAsignada: "cantera-banos",
    cargo: "Operador de Excavadora",
    ...overrides
  };
}

interface ErrorCallable {
  code: string;
  details?: { campo?: string };
}

async function capturarError(promesa: Promise<unknown>): Promise<ErrorCallable> {
  try {
    await promesa;
  } catch (error: unknown) {
    return error as ErrorCallable;
  }
  throw new Error("Se esperaba que la invocación fallara");
}

async function limpiarEmuladores(): Promise<void> {
  const firestoreHost = process.env.FIRESTORE_EMULATOR_HOST;
  const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST;
  if (!firestoreHost || !authHost) {
    throw new Error("Ejecutar con `npm run test:integration` (requiere emuladores de Auth y Firestore).");
  }
  await fetch(
    `http://${firestoreHost}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`,
    { method: "DELETE" }
  );
  await fetch(`http://${authHost}/emulator/v1/projects/${PROJECT_ID}/accounts`, {
    method: "DELETE"
  });
}

async function auditoriasDe(uid: string): Promise<admin.firestore.DocumentData[]> {
  const snap = await admin
    .firestore()
    .collection("auditoria")
    .where("documentoId", "==", uid)
    .get();
  return snap.docs
    .map((d) => d.data())
    .sort((a, b) => a.fecha.toMillis() - b.fecha.toMillis());
}

beforeAll(() => {
  if (admin.apps.length === 0) {
    admin.initializeApp({ projectId: PROJECT_ID });
  }
});

afterAll(async () => {
  testEnv.cleanup();
  await Promise.all(admin.apps.map((app) => app?.delete()));
});

beforeEach(async () => {
  await limpiarEmuladores();
});

describe("gestionarUsuario — integración con emuladores (HU-02)", () => {
  it("CA1: crea la cuenta en Auth, inyecta el claim y guarda el perfil activo", async () => {
    const resultado = await wrapped(base(), contextoAdmin);

    const usuarioAuth = await admin.auth().getUser(resultado.uid);
    expect(usuarioAuth.email).toBe("juan.perez@silmay.ec");
    expect(usuarioAuth.disabled).toBe(false);
    expect(usuarioAuth.customClaims).toEqual({ role: "operador" });

    const perfil = (await admin.firestore().doc(`usuarios/${resultado.uid}`).get()).data();
    expect(perfil).toMatchObject({
      uid: resultado.uid,
      nombres: "Juan Pérez",
      cedula: "1710034065",
      telefono: "0991234567",
      email: "juan.perez@silmay.ec",
      rol: "operador",
      zonaAsignada: "cantera-banos",
      cargo: "Operador de Excavadora",
      estado: true
    });
  });

  it("CA1: normaliza el correo a minúsculas", async () => {
    const resultado = await wrapped(base({ email: "Juan.PEREZ@Silmay.ec" }), contextoAdmin);
    const perfil = (await admin.firestore().doc(`usuarios/${resultado.uid}`).get()).data();
    expect(perfil?.email).toBe("juan.perez@silmay.ec");
  });

  it("CA2: bloquea una cédula duplicada informando el campo (MSG-17)", async () => {
    await wrapped(base(), contextoAdmin);

    const error = await capturarError(
      wrapped(base({ email: "otro@silmay.ec" }), contextoAdmin)
    );

    expect(error.code).toBe("already-exists");
    expect(error.details?.campo).toBe("cedula");
  });

  it("CA2: bloquea un correo duplicado informando el campo (MSG-17)", async () => {
    await wrapped(base(), contextoAdmin);

    const error = await capturarError(
      wrapped(base({ cedula: "0926687856" }), contextoAdmin)
    );

    expect(error.code).toBe("already-exists");
    expect(error.details?.campo).toBe("email");
  });

  it("CA2: al actualizar, el propio correo y cédula no cuentan como duplicados", async () => {
    const { uid } = await wrapped(base(), contextoAdmin);

    const resultado = await wrapped(
      base({ uid, password: undefined, cargo: "Jefe de Frente" }),
      contextoAdmin
    );

    expect(resultado.success).toBe(true);
    const perfil = (await admin.firestore().doc(`usuarios/${uid}`).get()).data();
    expect(perfil?.cargo).toBe("Jefe de Frente");
  });

  it("CA2: al actualizar, no permite tomar la cédula de otro usuario", async () => {
    await wrapped(base(), contextoAdmin);
    const { uid } = await wrapped(
      base({ cedula: "0926687856", email: "maria@silmay.ec" }),
      contextoAdmin
    );

    const error = await capturarError(
      wrapped(base({ uid, cedula: "1710034065", email: "maria@silmay.ec", password: undefined }), contextoAdmin)
    );

    expect(error.code).toBe("already-exists");
    expect(error.details?.campo).toBe("cedula");
  });

  it("CA3: la baja lógica deshabilita Auth, revoca sesiones y preserva los registros históricos", async () => {
    const { uid } = await wrapped(base(), contextoAdmin);
    const jornadaId = "6f1c2b9e-3d4a-4b5c-9e8f-0a1b2c3d4e5f";
    await admin.firestore().doc(`jornadas/${jornadaId}`).set({ uuid: jornadaId, userId: uid });
    const antes = await admin.auth().getUser(uid);
    // tokensValidAfterTime tiene resolución de segundos: se espera >1 s para que la revocación sea observable.
    await new Promise((r) => setTimeout(r, 1100));

    await wrapped(base({ uid, estado: false, password: undefined }), contextoAdmin);

    const despues = await admin.auth().getUser(uid);
    expect(despues.disabled).toBe(true);
    expect(Date.parse(despues.tokensValidAfterTime ?? "")).toBeGreaterThan(
      Date.parse(antes.tokensValidAfterTime ?? "")
    );
    const perfil = (await admin.firestore().doc(`usuarios/${uid}`).get()).data();
    expect(perfil?.estado).toBe(false);
    expect((await admin.firestore().doc(`jornadas/${jornadaId}`).get()).exists).toBe(true);
  });

  it("CA3: un usuario dado de baja puede reactivarse", async () => {
    const { uid } = await wrapped(base(), contextoAdmin);
    await wrapped(base({ uid, estado: false, password: undefined }), contextoAdmin);

    await wrapped(base({ uid, estado: true, password: undefined }), contextoAdmin);

    expect((await admin.auth().getUser(uid)).disabled).toBe(false);
  });

  it("el cambio de rol actualiza el Custom Claim", async () => {
    const { uid } = await wrapped(base(), contextoAdmin);

    await wrapped(base({ uid, rol: "supervisor", password: undefined }), contextoAdmin);

    expect((await admin.auth().getUser(uid)).customClaims).toEqual({ role: "supervisor" });
  });

  it("CA4: cada alta y modificación deja una entrada inmutable en auditoria", async () => {
    const { uid } = await wrapped(base(), contextoAdmin);
    await wrapped(base({ uid, estado: false, password: undefined }), contextoAdmin);

    const entradas = await auditoriasDe(uid);

    expect(entradas).toHaveLength(2);
    const [alta, baja] = entradas;
    expect(alta).toMatchObject({ accion: "CREATE", coleccion: "usuarios", usuarioId: ADMIN_UID });
    expect(baja).toMatchObject({
      accion: "UPDATE",
      coleccion: "usuarios",
      usuarioId: ADMIN_UID,
      detalle: { bajaLogica: true, antes: { estado: true }, despues: { estado: false } }
    });
    for (const entrada of entradas) {
      expect(entrada.fecha).toBeInstanceOf(admin.firestore.Timestamp);
      expect(entrada.id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    }
  });

  it("actualizar un uid inexistente responde not-found", async () => {
    const error = await capturarError(
      wrapped(base({ uid: "no-existe", password: undefined }), contextoAdmin)
    );
    expect(error.code).toBe("not-found");
  });
});
