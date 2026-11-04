import * as functions from "firebase-functions";
import { v1 } from "@google-cloud/firestore";

/**
 * HT-12 / RNF-16: respaldo mensual completo de Cloud Firestore hacia Google Cloud Storage.
 *
 * Se ejecuta el día 1 de cada mes a las 03:00 (America/Guayaquil), fuera de la operación
 * de campo. Exporta TODAS las colecciones (`collectionIds: []`) a una carpeta fechada del
 * bucket de respaldos. Procedimiento de restauración: docs/runbooks/restauracion-firestore.md.
 */

export const CRON_RESPALDO = "0 3 1 * *";
export const ZONA_HORARIA = "America/Guayaquil";
export const CARPETA_RESPALDOS = "firestore";

export interface SolicitudExportacion {
  name: string;
  outputUriPrefix: string;
  collectionIds: string[];
}

export interface DependenciasRespaldo {
  proyecto: string | undefined;
  /** Bucket destino (`gs://...` o solo el nombre). Por defecto `<proyecto>-respaldos`. */
  bucket?: string;
  ahora: Date;
  /** Inicia la exportación y devuelve el nombre de la operación de larga duración. */
  exportar: (solicitud: SolicitudExportacion) => Promise<string>;
}

export interface ResultadoRespaldo {
  operacion: string;
  destino: string;
}

/** Normaliza el bucket a `gs://nombre` sin barra final. */
export function normalizarBucket(bucket: string): string {
  const nombre = bucket.trim().replace(/^gs:\/\//, "").replace(/\/+$/, "");
  if (!/^[a-z0-9][a-z0-9._-]{1,220}[a-z0-9]$/.test(nombre)) {
    throw new Error(`Nombre de bucket de respaldos no válido: '${bucket}'.`);
  }
  return `gs://${nombre}`;
}

/**
 * Marca de tiempo local (America/Guayaquil, UTC-5 sin horario de verano) para nombrar la
 * carpeta: `AAAA-MM-DDTHH-mm`. Única por ejecución para no sobrescribir un respaldo previo
 * si el cron se reintenta o se ejecuta manualmente el mismo mes.
 */
export function sello(fecha: Date): string {
  const partes = new Intl.DateTimeFormat("en-CA", {
    timeZone: ZONA_HORARIA,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23"
  }).formatToParts(fecha);
  const p = (tipo: Intl.DateTimeFormatPartTypes) =>
    partes.find((x) => x.type === tipo)?.value ?? "00";
  return `${p("year")}-${p("month")}-${p("day")}T${p("hour")}-${p("minute")}`;
}

export function construirSolicitud(
  proyecto: string,
  bucket: string,
  ahora: Date
): SolicitudExportacion {
  return {
    name: `projects/${proyecto}/databases/(default)`,
    outputUriPrefix: `${normalizarBucket(bucket)}/${CARPETA_RESPALDOS}/${sello(ahora)}`,
    // Vacío = todas las colecciones, incluidas las que se agreguen en sprints futuros.
    collectionIds: []
  };
}

/** Lógica del respaldo, sin dependencias del runtime (probada en respaldoMensual.test.ts). */
export async function ejecutarRespaldo(deps: DependenciasRespaldo): Promise<ResultadoRespaldo> {
  if (!deps.proyecto) {
    throw new Error("No se pudo determinar el ID del proyecto para el respaldo.");
  }
  const bucket = deps.bucket?.trim() || `${deps.proyecto}-respaldos`;
  const solicitud = construirSolicitud(deps.proyecto, bucket, deps.ahora);
  const operacion = await deps.exportar(solicitud);
  return { operacion, destino: solicitud.outputUriPrefix };
}

function proyectoActual(): string | undefined {
  return process.env.GCLOUD_PROJECT ?? process.env.GCP_PROJECT;
}

/** Cron mensual (Cloud Scheduler → Pub/Sub). Reintenta hasta 3 veces si la exportación falla. */
export const respaldoMensual = functions
  .region("southamerica-east1")
  .runWith({ timeoutSeconds: 120, memory: "256MB" })
  .pubsub.schedule(CRON_RESPALDO)
  .timeZone(ZONA_HORARIA)
  .retryConfig({ retryCount: 3, minBackoffDuration: "300s" })
  .onRun(async () => {
    const cliente = new v1.FirestoreAdminClient();
    try {
      const resultado = await ejecutarRespaldo({
        proyecto: proyectoActual(),
        bucket: process.env.RESPALDOS_BUCKET,
        ahora: new Date(),
        exportar: async (solicitud) => {
          const [operacion] = await cliente.exportDocuments(solicitud);
          return operacion.name ?? "(sin nombre)";
        }
      });
      functions.logger.info("Respaldo mensual de Firestore iniciado", resultado);
      return null;
    } catch (error) {
      // Se relanza para que Cloud Scheduler lo marque fallido y aplique retryConfig.
      functions.logger.error("Falló el respaldo mensual de Firestore", error);
      throw error;
    } finally {
      await cliente.close();
    }
  });
