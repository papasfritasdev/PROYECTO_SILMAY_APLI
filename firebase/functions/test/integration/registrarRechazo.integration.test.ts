/**
 * Integración de registrarRechazo contra Firestore Emulator (HU-09 CA3).
 * Ejecutar con: npm run test:integration
 */
import { afterAll, beforeAll, beforeEach, describe, expect, it } from "vitest";
import * as admin from "firebase-admin";
import fft from "firebase-functions-test";
import { registrarRechazo } from "../../src/registrarRechazo";

const PROJECT_ID = process.env.GCLOUD_PROJECT || "silmay-dev";
const testEnv = fft({ projectId: PROJECT_ID });
const wrapped = testEnv.wrap(registrarRechazo);

beforeAll(() => {
  if (admin.apps.length === 0) admin.initializeApp({ projectId: PROJECT_ID });
});

afterAll(async () => {
  testEnv.cleanup();
  await Promise.all(admin.apps.map((app) => app?.delete()));
});

beforeEach(async () => {
  await fetch(
    `http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`,
    { method: "DELETE" }
  );
});

describe("registrarRechazo — integración", () => {
  it("escribe una entrada PERMISSION_DENIED con el autor tomado del token", async () => {
    const { id } = await wrapped(
      { coleccion: "maquinaria", documentoId: "EX-01", operacion: "create", origen: "web" },
      { auth: { uid: "contador-1", token: { role: "contador" } } }
    );

    const entrada = (await admin.firestore().doc(`auditoria/${id}`).get()).data();
    expect(entrada).toMatchObject({
      id,
      accion: "PERMISSION_DENIED",
      coleccion: "maquinaria",
      documentoId: "EX-01",
      usuarioId: "contador-1",
      detalle: { operacion: "create", origen: "web", rol: "contador" }
    });
    expect(entrada?.fecha).toBeInstanceOf(admin.firestore.Timestamp);
    expect(id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
  });

  it("ignora cualquier usuarioId enviado en el payload", async () => {
    const payload = {
      coleccion: "jornadas",
      documentoId: "uuid-1",
      operacion: "update",
      origen: "android",
      usuarioId: "victima"
    } as unknown as Parameters<typeof wrapped>[0];

    const { id } = await wrapped(payload, { auth: { uid: "op-real", token: { role: "operador" } } });

    expect((await admin.firestore().doc(`auditoria/${id}`).get()).data()?.usuarioId).toBe("op-real");
  });
});
