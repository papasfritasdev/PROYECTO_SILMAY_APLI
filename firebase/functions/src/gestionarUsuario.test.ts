import { describe, it, expect, afterAll } from "vitest";
import fft from "firebase-functions-test";
import { gestionarUsuario, GestionarUsuarioData } from "./gestionarUsuario";

const testEnv = fft();

describe("Cloud Function Callable: gestionarUsuario (Pruebas de Seguridad y Guardrails)", () => {
  const wrapped = testEnv.wrap(gestionarUsuario);

  afterAll(() => {
    testEnv.cleanup();
  });

  const payloadValido: GestionarUsuarioData = {
    nombres: "Juan Perez",
    cedula: "1710034065",
    email: "juan.perez@silmay.ec",
    telefono: "0991234567",
    password: "Password123!",
    rol: "operador",
    zonaAsignada: "cantera-banos",
    cargo: "Operador de Excavadora"
  };

  it("debe rechazar la invocación si el usuario no está autenticado (unauthenticated)", async () => {
    // context sin propiedad auth
    await expect(wrapped(payloadValido, {})).rejects.toThrow(
      /Debe estar autenticado/
    );
  });

  it("debe denegar el acceso a usuarios que no posean rol 'admin' (permission-denied)", async () => {
    const contextOperador = {
      auth: {
        uid: "user-op-123",
        token: { role: "operador" }
      }
    };

    await expect(wrapped(payloadValido, contextOperador)).rejects.toThrow(
      /Permiso denegado/
    );

    const contextSupervisor = {
      auth: {
        uid: "user-sup-123",
        token: { role: "supervisor" }
      }
    };

    await expect(wrapped(payloadValido, contextSupervisor)).rejects.toThrow(
      /Permiso denegado/
    );

    const contextContador = {
      auth: {
        uid: "user-con-123",
        token: { role: "contador" }
      }
    };

    await expect(wrapped(payloadValido, contextContador)).rejects.toThrow(
      /Permiso denegado/
    );
  });

  it("debe rechazar payload con cédula ecuatoriana inválida (invalid-argument)", async () => {
    const contextAdmin = {
      auth: {
        uid: "admin-uid-123",
        token: { role: "admin" }
      }
    };

    const payloadCedulaInvalida = {
      ...payloadValido,
      cedula: "0000000000"
    };

    await expect(wrapped(payloadCedulaInvalida, contextAdmin)).rejects.toThrow(
      /La cédula ingresada no es una cédula ecuatoriana válida/
    );
  });

  it("debe rechazar payload con rol no permitido (invalid-argument)", async () => {
    const contextAdmin = {
      auth: {
        uid: "admin-uid-123",
        token: { role: "admin" }
      }
    };

    const payloadRolInvalido = {
      ...payloadValido,
      rol: "rol_inexistente" as unknown as "operador"
    };

    await expect(wrapped(payloadRolInvalido, contextAdmin)).rejects.toThrow(
      /El rol proporcionado no es válido/
    );
  });

  it("debe rechazar payload con correo inválido (invalid-argument)", async () => {
    const contextAdmin = {
      auth: {
        uid: "admin-uid-123",
        token: { role: "admin" }
      }
    };

    const payloadEmailInvalido = {
      ...payloadValido,
      email: "correo-invalido"
    };

    await expect(wrapped(payloadEmailInvalido, contextAdmin)).rejects.toThrow(
      /El formato del correo electrónico institucional es inválido/
    );
  });
  const contextAdminValidacion = {
    auth: { uid: "admin-uid-123", token: { role: "admin" } }
  };

  it("HU-02 CA1: rechaza contraseñas de menos de 8 caracteres o sin letras y números", async () => {
    await expect(
      wrapped({ ...payloadValido, password: "Abc123" }, contextAdminValidacion)
    ).rejects.toThrow(/al menos 8 caracteres e incluir letras y números/);
    await expect(
      wrapped({ ...payloadValido, password: "SoloLetrasLargas" }, contextAdminValidacion)
    ).rejects.toThrow(/al menos 8 caracteres e incluir letras y números/);
  });

  it("HU-02 CA1: la contraseña es obligatoria al crear", async () => {
    const sinPassword = { ...payloadValido };
    delete sinPassword.password;
    await expect(wrapped(sinPassword, contextAdminValidacion)).rejects.toThrow(/contraseña/);
  });

  it("HU-02 CA1: rechaza teléfonos que no sean ecuatorianos válidos", async () => {
    await expect(
      wrapped({ ...payloadValido, telefono: "12345" }, contextAdminValidacion)
    ).rejects.toThrow(/teléfono debe ser un número ecuatoriano válido/);
  });

  it("rechaza que un admin se dé de baja o se retire su propio rol", async () => {
    await expect(
      wrapped({ ...payloadValido, uid: "admin-uid-123", rol: "admin", estado: false }, contextAdminValidacion)
    ).rejects.toThrow(/no puede darse de baja/);
    await expect(
      wrapped({ ...payloadValido, uid: "admin-uid-123", rol: "operador" }, contextAdminValidacion)
    ).rejects.toThrow(/no puede darse de baja/);
  });
});
