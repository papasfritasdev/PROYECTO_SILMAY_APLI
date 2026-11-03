import { describe, it, expect, afterAll } from "vitest";
import fft from "firebase-functions-test";
import { registrarRechazo, RegistrarRechazoData } from "./registrarRechazo";

const testEnv = fft();

describe("Cloud Function Callable: registrarRechazo (validaciones)", () => {
  const wrapped = testEnv.wrap(registrarRechazo);
  const contexto = { auth: { uid: "op-1", token: { role: "operador" } } };
  const valido: RegistrarRechazoData = {
    coleccion: "maquinaria",
    documentoId: "EX-01",
    operacion: "create",
    origen: "web"
  };

  afterAll(() => {
    testEnv.cleanup();
  });

  it("rechaza invocaciones sin autenticación", async () => {
    await expect(wrapped(valido, {})).rejects.toThrow(/Debe estar autenticado/);
  });

  it("rechaza colecciones fuera del esquema canónico", async () => {
    await expect(
      wrapped({ ...valido, coleccion: "otra" as unknown as "maquinaria" }, contexto)
    ).rejects.toThrow(/colección canónica/);
  });

  it("exige documentoId no vacío", async () => {
    await expect(wrapped({ ...valido, documentoId: "  " }, contexto)).rejects.toThrow(/documentoId/);
    await expect(wrapped({ ...valido, documentoId: "x".repeat(1501) }, contexto)).rejects.toThrow(
      /documentoId/
    );
  });

  it("valida operación y origen", async () => {
    await expect(
      wrapped({ ...valido, operacion: "drop" as unknown as "create" }, contexto)
    ).rejects.toThrow(/operación/);
    await expect(
      wrapped({ ...valido, origen: "ios" as unknown as "web" }, contexto)
    ).rejects.toThrow(/origen/);
  });
});
