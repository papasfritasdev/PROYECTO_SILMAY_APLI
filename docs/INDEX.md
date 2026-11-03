# Índice de Contexto y Navegación Técnica — Sistema SILMAY

Este índice define la ubicación y propósito de cada especificación técnica en el repositorio. Consulta únicamente el archivo correspondiente a tu tarea actual.

---

## 1. Arquitectura y Datos Compartidos

- `docs/architecture.md`: Estilos arquitectónicos (Clean Architecture + MVVM), separación de capas (`domain`, `data`, `presentation`), estructura física del Monorepo y ADRs aceptados (ADR-0001 a ADR-0010).
- `docs/firebase-schema.md`: Definición estricta de las 12 colecciones de Cloud Firestore, tipos de datos, metadatos obligatorios de control (`uuid`, `createdAtClient`, `syncedAtServer`, `userId`, `deviceId`, `zona`) e índices compuestos en `southamerica-east1`.
- `docs/cloud-functions.md`: Microservicios serverless en TypeScript: triggers reactivos (`procesarJornada`, `registrarAuditoria`), funciones HTTPS callables (`gestionarUsuario`, `consolidarPeriodo`) y cron de respaldo mensual.
- `docs/sync-protocol.md`: Ciclo de vida Offline-First, cola de mutaciones FIFO en disco, reintentos con backoff exponencial, prevención de colisiones por UUID v4 y medición de la métrica TES ≥ 99.0%.
- `docs/business-formulas.md`: Fórmulas matemáticas oficiales de cálculo: Horas Efectivas, Delta Horómetro, Conciliación `P_reg`, Consolidación de Resultado Operativo y Semáforo Preventivo.
- `docs/messages-catalog.md`: Catálogo oficial e inmutable de mensajes normalizados de validación e interfaz (`MSG-01` a `MSG-17`).
- `docs/setup-environment.md`: Configuración de proyectos aislados (`silmay-dev` y `silmay-prod`), emuladores locales de Firebase Suite y variables de entorno.

---

## 2. Manual de Operaciones del Equipo (Playbook)

- `docs/playbook/01-git-workflow.md`: Estrategia de ramas (`main`, `develop`, `feature/*`), convenciones de commits y políticas de Pull Request con verificación de CI.
- `docs/playbook/02-prompt-templates.md`: Plantillas estandarizadas de prompts para iniciar tareas o cerrar sprints con Google Antigravity y Claude.
- `docs/playbook/03-handover-guide.md`: Guía y estructura técnica obligatoria para redactar los informes de relevo entre sprints.
- `docs/playbook/04-ai-guardrails.md`: Restricciones arquitectónicas inviolables, prohibiciones de librerías y criterios obligatorios de la Definición de Terminado (DoD).

---

## 3. Relevo de Sprints (Sprints Handover)

- `docs/sprints-handover/handover-sprint-01.md`: Reporte de cierre técnico del Sprint 1 (punto de partida para el Sprint 2).
- `docs/sprints-handover/handover-sprint-02.md`: Reporte de cierre técnico del Sprint 2 (punto de partida para el Sprint 3).
- `docs/sprints-handover/handover-sprint-03.md`: Reporte de cierre técnico del Sprint 3 (punto de partida para el Sprint 4).
- `docs/sprints-handover/handover-sprint-04.md`: Reporte de cierre técnico del Sprint 4 (punto de partida para el Sprint 5).
- `docs/sprints-handover/handover-sprint-05.md`: Reporte de cierre técnico y auditoría final del Sprint 5.

---

## 4. Especificaciones Funcionales por Sprint (Specs)

- `docs/specs/sprint-01-specs.md`: Historias HT-01 a HT-04, HT-06 y HU-01a (Cimientos, CI, login móvil y base Android).
- `docs/specs/sprint-02-specs.md`: Historias HT-05, HT-07, HT-08, HT-12, HU-01b, HU-02 y HU-09 (Security Rules, base web y CRUDs).
- `docs/specs/sprint-03-specs.md`: Historias HU-07, HU-03, HU-04 y HU-08 (Captura Offline de jornadas, despachos y sync diferida).
- `docs/specs/sprint-04-specs.md`: Historias HU-05, HU-06, HT-09, HU-10 y HU-11 (Gastos diarios, novedades, mantenimiento y alertas).
- `docs/specs/sprint-05-specs.md`: Historias HT-10, HT-11 y HU-12 a HU-16 (Consolidación periódica, validación contable y reportes).