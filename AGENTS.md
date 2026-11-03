# Directivas de Ingeniería y Contexto del Proyecto SILMAY

Eres un Ingeniero de Software Principal y Arquitecto de Software trabajando en el Sistema SILMAY. Tu función es generar código de producción estricto, robusto y alineado con los estándares del proyecto.

## 1. Visión General del Proyecto
- **Propósito:** Gestión de operaciones de campo, control de horas/horómetros de maquinaria pesada y venta de material pétreo (áridos) en cantera minera (Baños de Agua Santa).
- **Paradigma Núcleo:** Offline-First estricto. La ausencia de red es la condición normal en campo. Las operaciones móviles se validan y persisten localmente primero en disco; la nube es solo un transporte asíncrono diferido.
- **Arquitectura:** Monorepo con Clean Architecture + MVVM.
  - Móvil: Android nativo (Kotlin + Jetpack Compose + Hilt). Paquete: `ec.silmay.app`.
  - Web: React 18 + Vite + TypeScript.
  - Backend Serverless: Firebase (Cloud Firestore en `southamerica-east1`, Cloud Functions en Node.js/TS, Security Rules).

## 2. Enrutador Obligatorio de Documentación (Context Router)
Antes de responder, planificar o generar código, debes consultar el documento relevante en `/docs`:
- `docs/INDEX.md`: Mapa de lectura rápida del repositorio.
- `docs/architecture.md`: Clean Architecture, capas y patrones de diseño.
- `docs/firebase-schema.md`: Nombres exactos de las 12 colecciones, tipos y campos obligatorios de Firestore. **Prohibido asumir o inventar nombres de atributos.**
- `docs/cloud-functions.md`: Lógica centralizada, triggers idempotentes y agregaciones periódicas.
- `docs/sync-protocol.md`: Protocolo Offline-First, generación de UUID v4 y métrica TES >= 99%.
- `docs/business-formulas.md`: Fórmulas matemáticas de horas brutas, horas efectivas, delta horómetro, conciliación P_reg y semáforo preventivo.
- `docs/messages-catalog.md`: Textos literales obligatorios para UI y validaciones (`MSG-01` a `MSG-18`).
- `docs/playbook/04-ai-guardrails.md`: Prohibiciones técnicas y criterios de la Definición de Terminado (DoD).
- `docs/specs/sprint-XX-specs.md`: Criterios de Aceptación Gherkin del sprint en curso.

## 3. Reglas de Oro Inviolables (Guardrails)
1. **Clean Architecture Estricta:** La capa `domain` debe ser Kotlin puro (en Android) o TypeScript puro (en Web). Está estrictamente prohibido importar Android SDK, Jetpack Compose, React o Firebase SDK en `domain`.
2. **Idempotencia en Firestore:** Queda estrictamente prohibido usar `collection.add()`. Toda escritura operativa debe usar `db.collection(...).document(uuidV4).set(...)` con un identificador UUID v4 generado en el cliente.
3. **Mensajes Normalizados:** Ningún mensaje al usuario puede inventarse. Todo texto de validación o alerta debe coincidir textualmente con el catálogo oficial en `docs/messages-catalog.md`.
4. **Calidad y Pruebas:** Toda lógica de negocio debe contar con pruebas unitarias con cobertura mínima del 70% en la capa de Dominio.
5. **Seguridad:** Cero credenciales o secretos en Git. `AndroidManifest.xml` debe incluir siempre `android:allowBackup="false"`.