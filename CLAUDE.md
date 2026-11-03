# CLAUDE.md - Directrices de Desarrollo para Claude (SILMAY Monorepo)

Este archivo establece las reglas operativas, arquitectura y contexto mandatorios para el desarrollo del Sistema SILMAY en este repositorio.

## Rol y Comportamiento Esperado
Actúa como Arquitecto de Software e Ingeniero Senior. Antes de escribir código o proponer refactorizaciones, valida tus decisiones contra la documentación técnica ubicada en `/docs`.

## Arquitectura y Stack
- **Monorepo:** `android/` (Kotlin, Jetpack Compose, Hilt, MVVM), `web/` (React 18, Vite, TypeScript), `firebase/` (Functions TypeScript, Rules, Indexes) y `scripts/`.
- **Offline-First:** Las capturas móviles ocurren sin red y se guardan localmente primero. La conectividad es un canal asíncrono.
- **Región de Base de Datos:** Cloud Firestore en `southamerica-east1`.
- **Idempotencia:** No usar `add()`. Usar siempre `.document(uuidV4).set(...)` con UUID v4 generado en cliente.

## Enrutamiento de Contexto
No intentes leer todo el repositorio. Consulta específicamente el archivo necesario según la tarea:
- Consulta `docs/INDEX.md` para identificar qué documento rige cada módulo.
- Consulta `docs/firebase-schema.md` antes de crear modelos, repositorios o consultas.
- Consulta `docs/business-formulas.md` antes de implementar cálculos de tiempo, costos o maquinaria.
- Consulta `docs/messages-catalog.md` para obtener los textos exactos de interfaz (códigos MSG-01 a MSG-17).
- Consulta `docs/playbook/04-ai-guardrails.md` para respetar la Definición de Terminado (DoD).
- Consulta `docs/specs/sprint-XX-specs.md` para validar los Criterios de Aceptación Gherkin del sprint.

## Restricciones Obligatorias
- **Independencia de Frameworks:** Prohibido importar dependencias de Android SDK, Compose, React o Firebase dentro de paquetes `domain`.
- **Cobertura de Pruebas:** Mínimo 70% de cobertura en la capa de dominio.
- **Seguridad:** `android:allowBackup="false"` obligatorio. Secretos fuera de Git.