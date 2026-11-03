/**
 * Batería de pruebas de Security Rules en Firestore Emulator (HT-05, Sprint 2).
 * Ejecutar con: npm run test:rules  (levanta el emulador vía firebase emulators:exec)
 *
 * Fuentes: docs/firebase-schema.md, docs/sync-protocol.md, docs/specs/sprint-02-specs.md
 */
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { afterAll, beforeAll, beforeEach, describe, it } from "vitest";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
  RulesTestEnvironment
} from "@firebase/rules-unit-testing";
import {
  deleteDoc,
  doc,
  DocumentData,
  getDoc,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc
} from "firebase/firestore";

type Firestore = ReturnType<ReturnType<RulesTestEnvironment["authenticatedContext"]>["firestore"]>;

const ZONA = "cantera-banos";
const OTRA_ZONA = "zona-norte";

interface PerfilPrueba {
  uid: string;
  rol: "operador" | "supervisor" | "contador" | "admin";
  zonaAsignada: string;
  estado: boolean;
}

const PERFILES: PerfilPrueba[] = [
  { uid: "admin1", rol: "admin", zonaAsignada: "administracion", estado: true },
  { uid: "contador1", rol: "contador", zonaAsignada: "administracion", estado: true },
  { uid: "op1", rol: "operador", zonaAsignada: ZONA, estado: true },
  { uid: "op2", rol: "operador", zonaAsignada: ZONA, estado: true },
  { uid: "opInactivo", rol: "operador", zonaAsignada: ZONA, estado: false },
  { uid: "sup1", rol: "supervisor", zonaAsignada: ZONA, estado: true },
  { uid: "supNorte", rol: "supervisor", zonaAsignada: OTRA_ZONA, estado: true }
];

let env: RulesTestEnvironment;

function db(uid: string): Firestore {
  const perfil = PERFILES.find((p) => p.uid === uid);
  if (!perfil) throw new Error(`Perfil de prueba inexistente: ${uid}`);
  return env.authenticatedContext(uid, { role: perfil.rol }).firestore();
}

function anonimo(): Firestore {
  return env.unauthenticatedContext().firestore();
}

async function sembrar(path: string, data: DocumentData): Promise<void> {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), path), data);
  });
}

const ahora = Date.now();
const ts = (offsetHoras: number): Timestamp =>
  Timestamp.fromMillis(ahora + offsetHoras * 3_600_000);

function control(uuid: string, userId: string, zona: string = ZONA): DocumentData {
  return {
    uuid,
    createdAtClient: ts(0),
    syncedAtServer: serverTimestamp(),
    userId,
    deviceId: "device-test-01",
    zona
  };
}

function jornadaAbierta(uuid: string, userId = "op1", zona = ZONA): DocumentData {
  return {
    ...control(uuid, userId, zona),
    maquinaId: "EX-01",
    lugarId: "frente-1",
    horaInicio: ts(-8),
    horaSalida: null,
    horometroInicial: 1250.5,
    horometroFinal: null,
    horasBrutas: null,
    horasRestadas: 0,
    horasEfectivas: null,
    deltaHorometro: null,
    observaciones: null,
    estado: "abierta",
    tieneFallaCritica: false,
    notificadaFalla: false,
    procesada: false,
    deducciones: []
  };
}

function jornadaCerrada(uuid: string, userId = "op1"): DocumentData {
  return {
    ...jornadaAbierta(uuid, userId),
    horaSalida: ts(0),
    horometroFinal: 1258.0,
    horasBrutas: 8.0,
    horasRestadas: 1.0,
    horasEfectivas: 7.0,
    deltaHorometro: 7.5,
    estado: "cerrada"
  };
}

function despacho(uuid: string, userId = "op1"): DocumentData {
  return {
    ...control(uuid, userId),
    materialId: "arena-fina",
    unidadMedida: "m3",
    cantidad: 12.5,
    clienteId: "cliente-1",
    volquetaId: "VOL-03",
    lugarId: "frente-1",
    metodoCobro: "efectivo",
    valorUSD: 180.0,
    fechaHora: ts(0),
    estadoValidacion: "pendiente",
    validadoPor: null,
    fechaValidacion: null,
    comentarioValidacion: null
  };
}

function gasto(uuid: string, userId = "op1"): DocumentData {
  return {
    ...control(uuid, userId),
    rubro: "combustible",
    descripcion: null,
    montoUSD: 45.5,
    numeroComprobante: "001-002-000123",
    unidadId: "EX-01",
    fechaHora: ts(0),
    estadoValidacion: "pendiente",
    validadoPor: null,
    fechaValidacion: null,
    comentarioValidacion: null
  };
}

function maquina(codigo: string, overrides: DocumentData = {}): DocumentData {
  return {
    codigoInterno: codigo,
    placa: null,
    marca: "Caterpillar",
    modelo: "320D",
    tipoEquipo: "excavadora",
    horometroBase: 1000.0,
    horometroActual: 1000.0,
    intervaloMantenimiento: 250,
    proximoMantenimientoHoras: 1250.0,
    estado: "operativa",
    ...overrides
  };
}

const UUID_A = "6f1c2b9e-3d4a-4b5c-9e8f-0a1b2c3d4e5f";
const UUID_B = "7a2d3c0f-4e5b-4c6d-8f90-1b2c3d4e5f60";

beforeAll(async () => {
  env = await initializeTestEnvironment({
    projectId: "silmay-dev",
    firestore: {
      rules: readFileSync(resolve(process.cwd(), "../firestore.rules"), "utf8")
    }
  });
});

afterAll(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  for (const p of PERFILES) {
    await sembrar(`usuarios/${p.uid}`, {
      uid: p.uid,
      nombres: `Usuario ${p.uid}`,
      cedula: "1710034065",
      telefono: null,
      email: `${p.uid}@silmay.ec`,
      rol: p.rol,
      tipoLicencia: null,
      zonaAsignada: p.zonaAsignada,
      cargo: "Prueba",
      estado: p.estado
    });
  }
});

// =============================================================================
describe("usuarios", () => {
  it("el usuario lee su propio perfil", async () => {
    await assertSucceeds(getDoc(doc(db("op1"), "usuarios/op1")));
  });

  it("un usuario dado de baja aún puede leer su perfil (para mostrar MSG-03)", async () => {
    await assertSucceeds(getDoc(doc(db("opInactivo"), "usuarios/opInactivo")));
  });

  it("un operador no puede leer el perfil de otro usuario", async () => {
    await assertFails(getDoc(doc(db("op1"), "usuarios/op2")));
  });

  it("el admin lee cualquier perfil", async () => {
    await assertSucceeds(getDoc(doc(db("admin1"), "usuarios/op1")));
  });

  it("un anónimo no puede leer perfiles", async () => {
    await assertFails(getDoc(doc(anonimo(), "usuarios/op1")));
  });

  it("ningún cliente (ni el admin) escribe en usuarios: solo gestionarUsuario", async () => {
    await assertFails(updateDoc(doc(db("op1"), "usuarios/op1"), { rol: "admin" }));
    await assertFails(updateDoc(doc(db("admin1"), "usuarios/op1"), { estado: false }));
  });

  it("prohibido el borrado físico de usuarios", async () => {
    await assertFails(deleteDoc(doc(db("admin1"), "usuarios/op1")));
  });
});

// =============================================================================
describe("jornadas", () => {
  it("el operador crea una jornada abierta válida con UUID v4 como ID", async () => {
    await assertSucceeds(setDoc(doc(db("op1"), `jornadas/${UUID_A}`), jornadaAbierta(UUID_A)));
  });

  it("el supervisor también puede registrar jornadas en móvil", async () => {
    await assertSucceeds(
      setDoc(doc(db("sup1"), `jornadas/${UUID_A}`), jornadaAbierta(UUID_A, "sup1"))
    );
  });

  it("rechaza uuid distinto del Document ID", async () => {
    await assertFails(setDoc(doc(db("op1"), `jornadas/${UUID_B}`), jornadaAbierta(UUID_A)));
  });

  it("rechaza suplantar el userId de otro operador", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), jornadaAbierta(UUID_A, "op2"))
    );
  });

  it("rechaza una zona distinta a la zonaAsignada del usuario (ADR-0010)", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), jornadaAbierta(UUID_A, "op1", OTRA_ZONA))
    );
  });

  it("rechaza syncedAtServer que no sea serverTimestamp()", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), {
        ...jornadaAbierta(UUID_A),
        syncedAtServer: ts(0)
      })
    );
  });

  it("rechaza que el cliente marque procesada o notificadaFalla en true", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), { ...jornadaAbierta(UUID_A), procesada: true })
    );
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), {
        ...jornadaAbierta(UUID_A),
        notificadaFalla: true
      })
    );
  });

  it("rechaza campos fuera del esquema canónico", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), { ...jornadaAbierta(UUID_A), extra: 1 })
    );
  });

  it("rechaza campos obligatorios faltantes", async () => {
    const incompleta = jornadaAbierta(UUID_A);
    delete incompleta.deviceId;
    await assertFails(setDoc(doc(db("op1"), `jornadas/${UUID_A}`), incompleta));
  });

  it("contador y admin no capturan jornadas (roles web)", async () => {
    await assertFails(
      setDoc(doc(db("contador1"), `jornadas/${UUID_A}`), jornadaAbierta(UUID_A, "contador1"))
    );
    await assertFails(
      setDoc(doc(db("admin1"), `jornadas/${UUID_A}`), jornadaAbierta(UUID_A, "admin1"))
    );
  });

  it("baja lógica: un operador con estado false no puede escribir", async () => {
    await assertFails(
      setDoc(doc(db("opInactivo"), `jornadas/${UUID_A}`), jornadaAbierta(UUID_A, "opInactivo"))
    );
  });

  it("acepta crear directamente una jornada cerrada coherente (captura offline)", async () => {
    await assertSucceeds(setDoc(doc(db("op1"), `jornadas/${UUID_A}`), jornadaCerrada(UUID_A)));
  });

  it("MSG-05: rechaza horometroFinal <= horometroInicial", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), {
        ...jornadaCerrada(UUID_A),
        horometroFinal: 1250.5
      })
    );
  });

  it("MSG-11: rechaza horaSalida <= horaInicio", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), {
        ...jornadaCerrada(UUID_A),
        horaSalida: ts(-8)
      })
    );
  });

  it("MSG-12: rechaza horasRestadas >= horasBrutas", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), {
        ...jornadaCerrada(UUID_A),
        horasRestadas: 8.0
      })
    );
  });

  it("rechaza una jornada abierta con datos de cierre", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), {
        ...jornadaAbierta(UUID_A),
        horometroFinal: 1300
      })
    );
  });

  it("el titular cierra su jornada abierta", async () => {
    await sembrar(`jornadas/${UUID_A}`, { ...jornadaAbierta(UUID_A), syncedAtServer: ts(0) });
    await assertSucceeds(setDoc(doc(db("op1"), `jornadas/${UUID_A}`), jornadaCerrada(UUID_A)));
  });

  it("una jornada cerrada es inmutable desde el móvil", async () => {
    await sembrar(`jornadas/${UUID_A}`, { ...jornadaCerrada(UUID_A), syncedAtServer: ts(0) });
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), {
        ...jornadaCerrada(UUID_A),
        observaciones: "cambio tardío"
      })
    );
  });

  it("RNF-11: el reintento idéntico de una jornada cerrada se acepta (solo cambia syncedAtServer)", async () => {
    await sembrar(`jornadas/${UUID_A}`, { ...jornadaCerrada(UUID_A), syncedAtServer: ts(-1) });
    await assertSucceeds(setDoc(doc(db("op1"), `jornadas/${UUID_A}`), jornadaCerrada(UUID_A)));
  });

  it("al actualizar no se pueden alterar campos inmutables (maquinaId, horometroInicial)", async () => {
    await sembrar(`jornadas/${UUID_A}`, { ...jornadaAbierta(UUID_A), syncedAtServer: ts(0) });
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), { ...jornadaAbierta(UUID_A), maquinaId: "EX-02" })
    );
    await assertFails(
      setDoc(doc(db("op1"), `jornadas/${UUID_A}`), {
        ...jornadaAbierta(UUID_A),
        horometroInicial: 1000
      })
    );
  });

  it("otro operador no puede modificar una jornada ajena", async () => {
    await sembrar(`jornadas/${UUID_A}`, { ...jornadaAbierta(UUID_A), syncedAtServer: ts(0) });
    await assertFails(setDoc(doc(db("op2"), `jornadas/${UUID_A}`), jornadaCerrada(UUID_A)));
  });

  it("lectura: titular, supervisor de su zona, contador y admin", async () => {
    await sembrar(`jornadas/${UUID_A}`, { ...jornadaAbierta(UUID_A), syncedAtServer: ts(0) });
    await assertSucceeds(getDoc(doc(db("op1"), `jornadas/${UUID_A}`)));
    await assertSucceeds(getDoc(doc(db("sup1"), `jornadas/${UUID_A}`)));
    await assertSucceeds(getDoc(doc(db("contador1"), `jornadas/${UUID_A}`)));
    await assertSucceeds(getDoc(doc(db("admin1"), `jornadas/${UUID_A}`)));
  });

  it("lectura denegada a otro operador y a supervisor de otra zona", async () => {
    await sembrar(`jornadas/${UUID_A}`, { ...jornadaAbierta(UUID_A), syncedAtServer: ts(0) });
    await assertFails(getDoc(doc(db("op2"), `jornadas/${UUID_A}`)));
    await assertFails(getDoc(doc(db("supNorte"), `jornadas/${UUID_A}`)));
    await assertFails(getDoc(doc(anonimo(), `jornadas/${UUID_A}`)));
  });

  it("prohibido el borrado físico de jornadas", async () => {
    await sembrar(`jornadas/${UUID_A}`, { ...jornadaAbierta(UUID_A), syncedAtServer: ts(0) });
    await assertFails(deleteDoc(doc(db("op1"), `jornadas/${UUID_A}`)));
    await assertFails(deleteDoc(doc(db("admin1"), `jornadas/${UUID_A}`)));
  });
});

// =============================================================================
describe("despachos", () => {
  it("el operador registra un despacho válido en estado pendiente", async () => {
    await assertSucceeds(setDoc(doc(db("op1"), `despachos/${UUID_A}`), despacho(UUID_A)));
  });

  it("rechaza cantidad <= 0 y valorUSD <= 0", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `despachos/${UUID_A}`), { ...despacho(UUID_A), cantidad: 0 })
    );
    await assertFails(
      setDoc(doc(db("op1"), `despachos/${UUID_A}`), { ...despacho(UUID_A), valorUSD: -5 })
    );
  });

  it("rechaza enums fuera de catálogo (unidadMedida, metodoCobro)", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `despachos/${UUID_A}`), { ...despacho(UUID_A), unidadMedida: "kg" })
    );
    await assertFails(
      setDoc(doc(db("op1"), `despachos/${UUID_A}`), { ...despacho(UUID_A), metodoCobro: "cheque" })
    );
  });

  it("el móvil no puede crear un despacho ya validado", async () => {
    await assertFails(
      setDoc(doc(db("op1"), `despachos/${UUID_A}`), {
        ...despacho(UUID_A),
        estadoValidacion: "validado"
      })
    );
  });

  it("el contador valida modificando solo los 4 campos permitidos", async () => {
    await sembrar(`despachos/${UUID_A}`, { ...despacho(UUID_A), syncedAtServer: ts(0) });
    await assertSucceeds(
      updateDoc(doc(db("contador1"), `despachos/${UUID_A}`), {
        estadoValidacion: "validado",
        validadoPor: "contador1",
        fechaValidacion: serverTimestamp()
      })
    );
  });

  it("MSG-15: marcar 'observado' exige comentario", async () => {
    await sembrar(`despachos/${UUID_A}`, { ...despacho(UUID_A), syncedAtServer: ts(0) });
    const ref = doc(db("contador1"), `despachos/${UUID_A}`);
    await assertFails(
      updateDoc(ref, {
        estadoValidacion: "observado",
        validadoPor: "contador1",
        fechaValidacion: serverTimestamp()
      })
    );
    await assertSucceeds(
      updateDoc(ref, {
        estadoValidacion: "observado",
        validadoPor: "contador1",
        fechaValidacion: serverTimestamp(),
        comentarioValidacion: "El valor no coincide con la bitácora física."
      })
    );
  });

  it("el contador jamás altera campos operativos (valorUSD, cantidad)", async () => {
    await sembrar(`despachos/${UUID_A}`, { ...despacho(UUID_A), syncedAtServer: ts(0) });
    await assertFails(
      updateDoc(doc(db("contador1"), `despachos/${UUID_A}`), {
        estadoValidacion: "validado",
        validadoPor: "contador1",
        fechaValidacion: serverTimestamp(),
        valorUSD: 1
      })
    );
  });

  it("el contador no puede firmar la validación con el UID de otro", async () => {
    await sembrar(`despachos/${UUID_A}`, { ...despacho(UUID_A), syncedAtServer: ts(0) });
    await assertFails(
      updateDoc(doc(db("contador1"), `despachos/${UUID_A}`), {
        estadoValidacion: "validado",
        validadoPor: "admin1",
        fechaValidacion: serverTimestamp()
      })
    );
  });

  it("el operador no puede autovalidarse ni editar tras crear", async () => {
    await sembrar(`despachos/${UUID_A}`, { ...despacho(UUID_A), syncedAtServer: ts(0) });
    const ref = doc(db("op1"), `despachos/${UUID_A}`);
    await assertFails(
      updateDoc(ref, {
        estadoValidacion: "validado",
        validadoPor: "op1",
        fechaValidacion: serverTimestamp()
      })
    );
    await assertFails(updateDoc(ref, { valorUSD: 999 }));
  });

  it("RNF-11: el reintento idéntico del despacho se acepta", async () => {
    await sembrar(`despachos/${UUID_A}`, { ...despacho(UUID_A), syncedAtServer: ts(-1) });
    await assertSucceeds(setDoc(doc(db("op1"), `despachos/${UUID_A}`), despacho(UUID_A)));
  });

  it("prohibido el borrado físico de despachos", async () => {
    await sembrar(`despachos/${UUID_A}`, { ...despacho(UUID_A), syncedAtServer: ts(0) });
    await assertFails(deleteDoc(doc(db("admin1"), `despachos/${UUID_A}`)));
  });
});

// =============================================================================
describe("gastos", () => {
  it("el operador registra un gasto válido", async () => {
    await assertSucceeds(setDoc(doc(db("op1"), `gastos/${UUID_A}`), gasto(UUID_A)));
  });

  it("MSG-07: rechaza montoUSD <= 0", async () => {
    await assertFails(setDoc(doc(db("op1"), `gastos/${UUID_A}`), { ...gasto(UUID_A), montoUSD: 0 }));
  });

  it("rubro 'otros' exige descripción", async () => {
    await assertFails(setDoc(doc(db("op1"), `gastos/${UUID_A}`), { ...gasto(UUID_A), rubro: "otros" }));
    await assertSucceeds(
      setDoc(doc(db("op1"), `gastos/${UUID_A}`), {
        ...gasto(UUID_A),
        rubro: "otros",
        descripcion: "Compra de candado para bodega"
      })
    );
  });

  it("rechaza rubros fuera del enum", async () => {
    await assertFails(setDoc(doc(db("op1"), `gastos/${UUID_A}`), { ...gasto(UUID_A), rubro: "comida" }));
  });

  it("el contador valida un gasto", async () => {
    await sembrar(`gastos/${UUID_A}`, { ...gasto(UUID_A), syncedAtServer: ts(0) });
    await assertSucceeds(
      updateDoc(doc(db("contador1"), `gastos/${UUID_A}`), {
        estadoValidacion: "validado",
        validadoPor: "contador1",
        fechaValidacion: serverTimestamp()
      })
    );
  });
});

// =============================================================================
describe("maquinaria (HU-09)", () => {
  it("CA1: el admin registra una unidad con proximoMantenimientoHoras = base + intervalo", async () => {
    await assertSucceeds(setDoc(doc(db("admin1"), "maquinaria/EX-01"), maquina("EX-01")));
  });

  it("el Document ID debe ser el codigoInterno", async () => {
    await assertFails(setDoc(doc(db("admin1"), "maquinaria/OTRO"), maquina("EX-01")));
  });

  it("rechaza proximoMantenimientoHoras mal calculado", async () => {
    await assertFails(
      setDoc(doc(db("admin1"), "maquinaria/EX-01"), maquina("EX-01", { proximoMantenimientoHoras: 1300 }))
    );
  });

  it("una volqueta exige placa", async () => {
    await assertFails(
      setDoc(doc(db("admin1"), "maquinaria/VOL-03"), maquina("VOL-03", { tipoEquipo: "volqueta" }))
    );
    await assertSucceeds(
      setDoc(
        doc(db("admin1"), "maquinaria/VOL-03"),
        maquina("VOL-03", { tipoEquipo: "volqueta", placa: "TBA-1234" })
      )
    );
  });

  it("una unidad nueva debe ingresar como 'operativa'", async () => {
    await assertFails(
      setDoc(doc(db("admin1"), "maquinaria/EX-01"), maquina("EX-01", { estado: "inactiva" }))
    );
  });

  it("CA3: operador, supervisor y contador no pueden crear ni modificar maquinaria", async () => {
    await assertFails(setDoc(doc(db("op1"), "maquinaria/EX-01"), maquina("EX-01")));
    await assertFails(setDoc(doc(db("sup1"), "maquinaria/EX-01"), maquina("EX-01")));
    await assertFails(setDoc(doc(db("contador1"), "maquinaria/EX-01"), maquina("EX-01")));
    await sembrar("maquinaria/EX-01", maquina("EX-01"));
    await assertFails(updateDoc(doc(db("op1"), "maquinaria/EX-01"), { estado: "inactiva" }));
  });

  it("todos los usuarios activos leen la flota (listas del móvil)", async () => {
    await sembrar("maquinaria/EX-01", maquina("EX-01"));
    await assertSucceeds(getDoc(doc(db("op1"), "maquinaria/EX-01")));
    await assertFails(getDoc(doc(anonimo(), "maquinaria/EX-01")));
  });

  it("el admin da de baja lógica, pero no cambia el codigoInterno ni borra", async () => {
    await sembrar("maquinaria/EX-01", maquina("EX-01"));
    const ref = doc(db("admin1"), "maquinaria/EX-01");
    await assertSucceeds(updateDoc(ref, { estado: "inactiva" }));
    await assertFails(updateDoc(ref, { codigoInterno: "EX-99" }));
    await assertFails(deleteDoc(ref));
  });
});

// =============================================================================
describe("mantenimientos", () => {
  const mant = (id: string): DocumentData => ({
    id,
    maquinaId: "EX-01",
    fecha: ts(0),
    costoUSD: 320.0,
    repuestos: ["filtro de aceite"],
    horometroServicio: 1250.0,
    nuevoLimite: 1500.0,
    realizadoPor: "Taller Central"
  });

  it("solo el admin registra mantenimientos", async () => {
    await assertSucceeds(setDoc(doc(db("admin1"), `mantenimientos/${UUID_A}`), mant(UUID_A)));
    await assertFails(setDoc(doc(db("op1"), `mantenimientos/${UUID_B}`), mant(UUID_B)));
  });

  it("contador lee, operador no", async () => {
    await sembrar(`mantenimientos/${UUID_A}`, mant(UUID_A));
    await assertSucceeds(getDoc(doc(db("contador1"), `mantenimientos/${UUID_A}`)));
    await assertFails(getDoc(doc(db("op1"), `mantenimientos/${UUID_A}`)));
  });
});

// =============================================================================
describe("notificaciones (HU-11)", () => {
  const notif: DocumentData = {
    tipo: "falla_critica",
    referenciaId: UUID_A,
    mensaje: "Falla crítica reportada en máquina EX-01",
    severidad: "critica",
    fecha: ts(0),
    atendida: false,
    atendidaPor: null
  };

  it("ningún cliente crea notificaciones (solo procesarJornada)", async () => {
    await assertFails(setDoc(doc(db("admin1"), `notificaciones/falla_${UUID_A}`), notif));
  });

  it("CA3: el admin marca la alerta como atendida con su UID", async () => {
    await sembrar(`notificaciones/falla_${UUID_A}`, notif);
    await assertSucceeds(
      updateDoc(doc(db("admin1"), `notificaciones/falla_${UUID_A}`), {
        atendida: true,
        atendidaPor: "admin1"
      })
    );
  });

  it("CA4: el contador lee pero no puede atender alertas", async () => {
    await sembrar(`notificaciones/falla_${UUID_A}`, notif);
    const ref = doc(db("contador1"), `notificaciones/falla_${UUID_A}`);
    await assertSucceeds(getDoc(ref));
    await assertFails(updateDoc(ref, { atendida: true, atendidaPor: "contador1" }));
  });

  it("roles de campo no leen la bandeja de alertas", async () => {
    await sembrar(`notificaciones/falla_${UUID_A}`, notif);
    await assertFails(getDoc(doc(db("op1"), `notificaciones/falla_${UUID_A}`)));
  });
});

// =============================================================================
describe("conciliaciones y consolidados", () => {
  const conciliacion = (): DocumentData => ({
    periodo: "2026-10",
    tipo: "despachos",
    registrosValidados: 98,
    eventosReales: 100,
    pReg: 98.0,
    contadorId: "contador1",
    fecha: serverTimestamp()
  });

  it("el contador registra la conciliación; el admin no", async () => {
    await assertSucceeds(setDoc(doc(db("contador1"), "conciliaciones/2026-10"), conciliacion()));
    await assertFails(
      setDoc(doc(db("admin1"), "conciliaciones/2026-11"), { ...conciliacion(), contadorId: "admin1" })
    );
  });

  it("rechaza un período con formato inválido", async () => {
    await assertFails(
      setDoc(doc(db("contador1"), "conciliaciones/x"), { ...conciliacion(), periodo: "2026-13" })
    );
  });

  const consolidado: DocumentData = {
    estado: "borrador",
    resultadoOperativo: 1500.0,
    aprobadoPor: null,
    fechaAprobacion: null
  };

  it("ningún cliente crea consolidados (solo consolidarPeriodo)", async () => {
    await assertFails(setDoc(doc(db("admin1"), "consolidados/periodo_2026_10"), consolidado));
  });

  it("solo el admin aprueba y luego queda inmutable", async () => {
    await sembrar("consolidados/periodo_2026_10", consolidado);
    const aprobacion = {
      estado: "aprobado",
      aprobadoPor: "admin1",
      fechaAprobacion: serverTimestamp()
    };
    await assertFails(
      updateDoc(doc(db("contador1"), "consolidados/periodo_2026_10"), {
        ...aprobacion,
        aprobadoPor: "contador1"
      })
    );
    const ref = doc(db("admin1"), "consolidados/periodo_2026_10");
    await assertSucceeds(updateDoc(ref, aprobacion));
    await assertFails(updateDoc(ref, { resultadoOperativo: 0 }));
  });
});

// =============================================================================
describe("catalogos y parametros", () => {
  it("el admin agrega ítems a un catálogo válido", async () => {
    await assertSucceeds(
      setDoc(doc(db("admin1"), "catalogos/materiales/items/arena-fina"), {
        id: "arena-fina",
        nombre: "Arena fina",
        activo: true
      })
    );
  });

  it("rechaza tipos de catálogo no canónicos", async () => {
    await assertFails(
      setDoc(doc(db("admin1"), "catalogos/colores/items/rojo"), { id: "rojo", nombre: "Rojo", activo: true })
    );
  });

  it("el operador lee catálogos pero no los modifica", async () => {
    await sembrar("catalogos/lugares/items/frente-1", { id: "frente-1", nombre: "Frente 1", activo: true });
    await assertSucceeds(getDoc(doc(db("op1"), "catalogos/lugares/items/frente-1")));
    await assertFails(
      updateDoc(doc(db("op1"), "catalogos/lugares/items/frente-1"), { activo: false })
    );
  });

  it("prohibido borrar ítems de catálogo (baja lógica)", async () => {
    await sembrar("catalogos/lugares/items/frente-1", { id: "frente-1", nombre: "Frente 1", activo: true });
    await assertFails(deleteDoc(doc(db("admin1"), "catalogos/lugares/items/frente-1")));
  });

  it("parametros/sistema: umbral entre 0 y 1, solo admin", async () => {
    const ref = (uid: string) => doc(db(uid), "parametros/sistema");
    await assertSucceeds(
      setDoc(ref("admin1"), { umbralProximoMantenimiento: 0.9, zonaHoraria: "America/Guayaquil" })
    );
    await assertFails(
      setDoc(ref("admin1"), { umbralProximoMantenimiento: 1.5, zonaHoraria: "America/Guayaquil" })
    );
    await assertFails(
      setDoc(ref("op1"), { umbralProximoMantenimiento: 0.8, zonaHoraria: "America/Guayaquil" })
    );
    await assertSucceeds(getDoc(ref("op1")));
  });

  it("solo existe el documento 'sistema' en parametros", async () => {
    await assertFails(
      setDoc(doc(db("admin1"), "parametros/otro"), {
        umbralProximoMantenimiento: 0.9,
        zonaHoraria: "America/Guayaquil"
      })
    );
  });
});

// =============================================================================
describe("auditoria y rutas no declaradas", () => {
  it("auditoria: lectura solo admin; nadie escribe desde cliente", async () => {
    await sembrar("auditoria/a1", { accion: "CREATE", coleccion: "usuarios", documentoId: "op1" });
    await assertSucceeds(getDoc(doc(db("admin1"), "auditoria/a1")));
    await assertFails(getDoc(doc(db("contador1"), "auditoria/a1")));
    await assertFails(setDoc(doc(db("admin1"), "auditoria/a2"), { accion: "UPDATE" }));
  });

  it("denegación por defecto (spike_offline de HT-06 y colecciones desconocidas)", async () => {
    await assertFails(setDoc(doc(db("op1"), `spike_offline/${UUID_A}`), { uuid: UUID_A }));
    await assertFails(setDoc(doc(db("admin1"), "otra/x"), { a: 1 }));
  });
});
