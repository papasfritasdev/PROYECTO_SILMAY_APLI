import { describe, it, expect, vi } from "vitest";
import {
  construirSolicitud,
  ejecutarRespaldo,
  normalizarBucket,
  sello,
  CRON_RESPALDO,
  ZONA_HORARIA,
  respaldoMensual
} from "./respaldoMensual";

// 1 de noviembre de 2026, 03:00 en Guayaquil (UTC-5) = 08:00 UTC.
const EJECUCION = new Date("2026-11-01T08:00:00Z");

describe("respaldoMensual (HT-12, RNF-16)", () => {
  it("se programa el día 1 de cada mes a las 03:00 hora de Ecuador", () => {
    expect(CRON_RESPALDO).toBe("0 3 1 * *");
    expect(ZONA_HORARIA).toBe("America/Guayaquil");
    process.env.GCLOUD_PROJECT ??= "silmay-dev";
    const endpoint = (respaldoMensual as unknown as { __endpoint: Record<string, unknown> }).__endpoint;
    expect(endpoint.region).toEqual(["southamerica-east1"]);
    expect(endpoint.scheduleTrigger).toMatchObject({
      schedule: CRON_RESPALDO,
      timeZone: ZONA_HORARIA,
      retryConfig: { retryCount: 3, minBackoffDuration: "300s" }
    });
  });

  it("nombra la carpeta con la fecha y hora local de Guayaquil", () => {
    expect(sello(EJECUCION)).toBe("2026-11-01T03-00");
    // 31/12 23:30 en Guayaquil sigue siendo diciembre aunque en UTC ya sea enero.
    expect(sello(new Date("2027-01-01T04:30:00Z"))).toBe("2026-12-31T23-30");
  });

  it("exporta TODAS las colecciones de la base (default) a la carpeta fechada", () => {
    expect(construirSolicitud("silmay-prod", "silmay-prod-respaldos", EJECUCION)).toEqual({
      name: "projects/silmay-prod/databases/(default)",
      outputUriPrefix: "gs://silmay-prod-respaldos/firestore/2026-11-01T03-00",
      collectionIds: []
    });
  });

  it("acepta el bucket con o sin prefijo gs:// y rechaza nombres inválidos", () => {
    expect(normalizarBucket("gs://mi-bucket/")).toBe("gs://mi-bucket");
    expect(normalizarBucket(" mi.bucket_1 ")).toBe("gs://mi.bucket_1");
    expect(() => normalizarBucket("")).toThrow(/bucket/);
    expect(() => normalizarBucket("gs://Mayusculas")).toThrow(/bucket/);
    expect(() => normalizarBucket("a/b")).toThrow(/bucket/);
  });

  it("usa <proyecto>-respaldos si no se configura RESPALDOS_BUCKET", async () => {
    const exportar = vi.fn().mockResolvedValue("projects/silmay-dev/databases/(default)/operations/op-1");

    const resultado = await ejecutarRespaldo({ proyecto: "silmay-dev", ahora: EJECUCION, exportar });

    expect(exportar).toHaveBeenCalledOnce();
    expect(exportar.mock.calls[0][0].outputUriPrefix).toBe(
      "gs://silmay-dev-respaldos/firestore/2026-11-01T03-00"
    );
    expect(resultado).toEqual({
      operacion: "projects/silmay-dev/databases/(default)/operations/op-1",
      destino: "gs://silmay-dev-respaldos/firestore/2026-11-01T03-00"
    });
  });

  it("respeta el bucket configurado", async () => {
    const exportar = vi.fn().mockResolvedValue("op");

    await ejecutarRespaldo({ proyecto: "silmay-prod", bucket: "gs://otro", ahora: EJECUCION, exportar });

    expect(exportar.mock.calls[0][0].outputUriPrefix).toBe("gs://otro/firestore/2026-11-01T03-00");
  });

  it("falla sin proyecto y propaga errores de la exportación (para que el cron reintente)", async () => {
    const exportar = vi.fn();
    await expect(ejecutarRespaldo({ proyecto: undefined, ahora: EJECUCION, exportar })).rejects.toThrow(
      /proyecto/
    );
    expect(exportar).not.toHaveBeenCalled();

    await expect(
      ejecutarRespaldo({
        proyecto: "silmay-prod",
        ahora: EJECUCION,
        exportar: vi.fn().mockRejectedValue(new Error("PERMISSION_DENIED: importExportAdmin"))
      })
    ).rejects.toThrow(/importExportAdmin/);
  });
});
