# Especificación de Cloud Functions y Lógica Serverless — SILMAY

- **Documento Fuente:** Resumen Técnico de Diseño Arquitectónico SILMAY v2.0
- **Runtime:** Node.js (TypeScript)
- **Región:** `southamerica-east1`

---

## 1. Catálogo Oficial de Microservicios Serverless

| Función | Tipo / Disparador | Roles Autorizados | Responsabilidad Técnica | Requisitos |
| :--- | :--- | :--- | :--- | :--- |
| **`procesarJornada`** | Trigger `onDocumentWritten` en `jornadas/{id}` | Sistema (Automático) | Recalcula horas y delta horómetro; en transacción ACID suma el horómetro acumulado a la maquinaria; evalúa fallas críticas y alertas preventivas; idempotente mediante bandera `procesada`. | RF-20, RF-21, RNF-11 |
| **`consolidarPeriodo`** | Invocable (HTTPS callable) | `admin`, `contador` | Totaliza volúmenes de material, ingresos por cliente/método, horas máquina/operador, horas restadas y gastos por rubro. Calcula el Resultado Operativo y persiste en `consolidados`. | RF-25, RF-26, RNF-02 |
| **`gestionarUsuario`** | Invocable (HTTPS callable) | `admin` | Crea/actualiza cuenta en Firebase Auth, asigna el Custom Claim `role`, persiste en colección `usuarios` y genera bitácora en `auditoria`. | RF-05, RNF-08 |
| **`registrarAuditoria`** | Trigger `onDocumentWritten` en `maquinaria`, `catalogos`, `parametros` | Sistema (Automático) | Captura cambios administrativos y genera una entrada inmutable append-only con `serverTimestamp` en `auditoria`. | RNF-08 |
| **`respaldoMensual`** | Tarea cron programada (Pub/Sub) | Sistema (Schedule) | Ejecuta la exportación automatizada de todas las colecciones hacia un bucket seguro en Google Cloud Storage. | RNF-16 |

---

## 2. Detalle de Implementación de Triggers Críticos

### 2.1 Trigger `procesarJornada` (RF-20, RF-21)
Se dispara ante cualquier escritura en la ruta `jornadas/{uuid}`:

```typescript
// Lógica canónica requerida (Firebase Functions SDK v1)
export const procesarJornada = functions.region('southamerica-east1').firestore
  .document('jornadas/{id}')
  .onWrite(async (change, context) => {
    const after = change.after.data();
    if (!after) return; // Documento eliminado

    // Condición de ejecución: jornada cerrada que aún no haya sido procesada
    if (after.estado === 'cerrada' && after.procesada === false) {
      const jornadaRef = change.after.ref;
      const maquinaRef = admin.firestore().collection('maquinaria').doc(after.maquinaId);

      await admin.firestore().runTransaction(async (transaction) => {
        const maquinaDoc = await transaction.get(maquinaRef);
        if (!maquinaDoc.exists) throw new Error('Maquinaria no encontrada');

        const maquinaData = maquinaDoc.data()!;
        const deltaHorometro = after.deltaHorometro || 0;
        const nuevoHorometroActual = (maquinaData.horometroActual || 0) + deltaHorometro;

        // 1. Actualizar horómetro acumulado en maquinaria
        transaction.update(maquinaRef, {
          horometroActual: nuevoHorometroActual
        });

        // 2. Marcar jornada como procesada (idempotencia estricta)
        transaction.update(jornadaRef, {
          procesada: true
        });

        // 3. Evaluar alerta de falla crítica
        if (after.tieneFallaCritica === true && after.notificadaFalla === false) {
          const notifRef = admin.firestore().collection('notificaciones').doc(`falla_${context.params.id}`);
          transaction.set(notifRef, {
            tipo: 'falla_critica',
            referenciaId: context.params.id,
            mensaje: `Falla crítica reportada en máquina ${after.maquinaId}`,
            severidad: 'critica',
            fecha: admin.firestore.FieldValue.serverTimestamp(),
            atendida: false,
            atendidaPor: null
          });
          transaction.update(jornadaRef, { notificadaFalla: true });
        }

        // 4. Evaluar límite de mantenimiento preventivo
        if (nuevoHorometroActual >= maquinaData.proximoMantenimientoHoras) {
          transaction.update(maquinaRef, { estado: 'requiere_mantenimiento' });
          const notifMaintRef = admin.firestore().collection('notificaciones').doc(`maint_${after.maquinaId}_${Math.floor(nuevoHorometroActual)}`);
          transaction.set(notifMaintRef, {
            tipo: 'mantenimiento',
            referenciaId: after.maquinaId,
            mensaje: `Unidad ${after.maquinaId} alcanzó límite preventivo (${nuevoHorometroActual} hrs)`,
            severidad: 'advertencia',
            fecha: admin.firestore.FieldValue.serverTimestamp(),
            atendida: false,
            atendidaPor: null
          });
        }
      });
    }
  });
```

*(Implementación de lógica serverless para control de turnos y alertas).*

---

## 3. Garantías de Concurrencia e Idempotencia (RNF-11)

- **Protección ante reentrega de eventos:** Los triggers en Cloud Functions se ejecutan con semántica *at-least-once* (al menos una vez). La verificación de la bandera `procesada === false` combinada con una transacción ACID garantiza que los horómetros jamás se sumen dos veces ante fallos transitorios de red o reintentos.
- **IDs Deterministas en Notificaciones:** Para evitar generar alertas duplicadas en reintentos, el ID del documento en `notificaciones` se genera de forma determinista usando el patrón `falla_{jornadaUuid}` o `maint_{maquinaId}_{horometro}`.