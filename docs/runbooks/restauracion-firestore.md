# Runbook: Respaldo y Restauración de Cloud Firestore (HT-12, RNF-16)

> **Estado de validación:** el procedimiento se redactó contra la documentación oficial de
> Firestore import/export, pero **no se ha ejecutado en la nube**: por decisión de costos el
> equipo trabaja solo con Firebase Emulator Suite, que no implementa `exportDocuments` ni
> `gcloud firestore import`. La validación real (§5) queda pendiente para el primer despliegue
> en `silmay-dev`.

---

## 1. Qué se respalda y cuándo

- **Función:** `respaldoMensual` (`firebase/functions/src/respaldoMensual.ts`), región `southamerica-east1`.
- **Programación:** cron `0 3 1 * *` en `America/Guayaquil` → el día 1 de cada mes a las 03:00, fuera de la operación de campo. Cloud Scheduler reintenta hasta 3 veces (backoff mínimo de 5 min) si la exportación falla.
- **Alcance:** exportación administrada completa de la base `(default)` (`collectionIds: []`): las 12 colecciones canónicas, incluidas las subcolecciones `catalogos/{tipo}/items`, y cualquier colección futura.
- **Destino:** `gs://<bucket>/firestore/AAAA-MM-DDTHH-mm/` (hora local de Ecuador). Cada ejecución usa una carpeta nueva y nunca sobrescribe un respaldo anterior.
- **Bucket:** variable de entorno `RESPALDOS_BUCKET` de la función; si no se define, `<projectId>-respaldos` (p. ej. `silmay-prod-respaldos`).
- **No incluye:** cuentas de Firebase Authentication (se exportan aparte con `firebase auth:export`, ver §4.3) ni el código de las Functions o las reglas (están en Git).

## 2. Preparación única por entorno (antes del primer despliegue)

Ejecutar con un usuario con rol *Owner* del proyecto. Ejemplo para `silmay-prod`:

```powershell
$PROYECTO = "silmay-prod"
$BUCKET   = "gs://silmay-prod-respaldos"

# 1. Bucket en la MISMA región que Firestore (ADR-0009), acceso uniforme y sin acceso público.
gcloud storage buckets create $BUCKET --project=$PROYECTO --location=southamerica-east1 `
  --uniform-bucket-level-access --public-access-prevention

# 2. Retención: borrar respaldos con más de 365 días (ajustar según la política de SILMAY).
@'
{ "rule": [ { "action": { "type": "Delete" }, "condition": { "age": 365 } } ] }
'@ | Out-File -Encoding ascii lifecycle.json
gcloud storage buckets update $BUCKET --lifecycle-file=lifecycle.json

# 3. Permisos de la cuenta de servicio con la que corre la función
#    (Functions de 1.ª generación: <projectId>@appspot.gserviceaccount.com).
$SA = "$PROYECTO@appspot.gserviceaccount.com"
gcloud projects add-iam-policy-binding $PROYECTO `
  --member="serviceAccount:$SA" --role="roles/datastore.importExportAdmin"
gcloud storage buckets add-iam-policy-binding $BUCKET `
  --member="serviceAccount:$SA" --role="roles/storage.admin"

# 4. APIs necesarias para el cron (el despliegue las solicita si faltan).
gcloud services enable cloudscheduler.googleapis.com pubsub.googleapis.com --project=$PROYECTO
```

Si el bucket no se llama `<projectId>-respaldos`, definir `RESPALDOS_BUCKET` en
`firebase/functions/.env.<projectId>` (no contiene secretos) antes de desplegar.

## 3. Verificar que el respaldo mensual se ejecutó

```powershell
# Operaciones de exportación recientes (estado SUCCESSFUL / FAILED)
gcloud firestore operations list --project=$PROYECTO --filter="metadata.operationType=EXPORT_DOCUMENTS"

# Carpetas generadas
gcloud storage ls "$BUCKET/firestore/"

# Logs de la función
firebase functions:log --only respaldoMensual --project $PROYECTO
```

Ejecución manual fuera de calendario (p. ej. antes de una migración):
`gcloud scheduler jobs run firebase-schedule-respaldoMensual-southamerica-east1 --location=southamerica-east1 --project=$PROYECTO`
(el nombre exacto del job se lista con `gcloud scheduler jobs list`).

## 4. Restaurar

> **Regla:** nunca importar primero sobre producción. Restaurar en `silmay-dev`, validar (§5)
> y solo entonces decidir sobre `silmay-prod`.

### 4.1 Consideraciones antes de importar
- La importación **sobrescribe** los documentos con el mismo ID y **no borra** los documentos que no estén en el respaldo. Es una fusión documento a documento, no un "rollback" exacto de la base.
- Las escrituras importadas **disparan los triggers** (`procesarJornada`, `registrarAuditoria`, …). En particular, una jornada importada con `procesada == false` podría volver a sumar horómetro. Antes de importar, pausar o desplegar las funciones de trigger deshabilitadas en el proyecto destino.
- Mientras dure la importación, bloquear la captura de campo (las apps móviles seguirán encolando offline sin perder datos, sync-protocol §1).

### 4.2 Importación
```powershell
$ORIGEN  = "gs://silmay-prod-respaldos/firestore/2026-11-01T03-00"
$DESTINO = "silmay-dev"

# La cuenta de servicio de Firestore del proyecto DESTINO necesita leer el bucket de origen:
$NUM = gcloud projects describe $DESTINO --format="value(projectNumber)"
gcloud storage buckets add-iam-policy-binding gs://silmay-prod-respaldos `
  --member="serviceAccount:service-$NUM@gcp-sa-firestore.iam.gserviceaccount.com" `
  --role="roles/storage.objectViewer"

gcloud firestore import $ORIGEN --project=$DESTINO --async
gcloud firestore operations list --project=$DESTINO --filter="metadata.operationType=IMPORT_DOCUMENTS"
```

### 4.3 Cuentas de Authentication
Los perfiles de `usuarios` se restauran con Firestore, pero las cuentas y sus Custom Claims
viven en Firebase Authentication:
```powershell
firebase auth:export usuarios-auth.json --project silmay-prod   # respaldar (contiene hashes: tratar como secreto, NO versionar)
firebase auth:import usuarios-auth.json --project silmay-dev --hash-algo=SCRYPT ...  # parámetros de hash desde la consola
```
La exportación de Auth no está automatizada en HT-12 (fuera del alcance de RNF-16); queda como decisión abierta del equipo.

## 5. Validación de una restauración (pendiente de ejecutar en la nube)

1. Antes del respaldo, registrar el conteo por colección en origen (consola de Firestore o `count()` desde un script con Admin SDK).
2. Importar en `silmay-dev` (§4.2) y repetir el conteo: debe coincidir para las 12 colecciones.
3. Muestreo: abrir 5 documentos de `jornadas`, `despachos` y `gastos` y comparar campos, incluidos `createdAtClient`/`syncedAtServer` (los `Timestamp` se preservan).
4. Iniciar sesión con un operador y un admin restaurados (tras §4.3) y comprobar menú por rol y acceso RBAC.
5. Registrar fecha, carpeta de origen, conteos y resultado en el handover del sprint vigente.
