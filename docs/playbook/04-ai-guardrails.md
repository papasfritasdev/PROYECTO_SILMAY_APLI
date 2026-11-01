# Guardrails de IA y Definición de Terminado (DoD) — SILMAY

Este archivo establece los límites técnicos, prohibiciones y criterios de calidad que tanto Google Antigravity como Claude deben acatar de forma estricta. Ninguna tarea se considera terminada si incumple estos guardrails.

---

## 1. Definición de Terminado (Definition of Done - DoD)

Para dar por finalizada cualquier Historia de Usuario (HU) o Historia Técnica (HT), deben satisfacerse los siguientes 6 criterios de calidad técnica:

1. **Criterios de Aceptación Verificados:** Todos los escenarios en formato Gherkin (Dado / Cuando / Entonces) deben estar validados con pruebas automatizadas o funcionales.
2. **Cobertura de Pruebas Unitarias ≥ 70%:** La capa de dominio debe mantener una cobertura mínima del 70% verificada mediante Kover (Android) y Vitest (Web) en el pipeline de CI.
3. **Independencia de Frameworks (Clean Architecture):** La capa de Dominio no debe importar dependencias de Android SDK, Jetpack Compose, React ni Firebase SDK (RNF-14).
4. **Idempotencia y Persistencia en Borde:** Toda mutación de datos operativos debe generar su ID = UUID v4 en el cliente y persistirse mediante `set()` en la caché de Firestore. Las Security Rules asociadas deben validarse en el emulador local.
5. **Mensajes Normalizados:** Toda alerta o notificación al usuario debe coincidir exactamente con el catálogo oficial `docs/messages-catalog.md` (`MSG-01` a `MSG-16`).
6. **Seguridad del Repositorio:** Cero credenciales, tokens o archivos `.env` versionados en Git. `AndroidManifest.xml` debe declarar obligatoriamente `android:allowBackup="false"`.

---

## 2. Prohibiciones Técnicas Absolutas para las IAs

### 2.1 En Base de Datos y Firebase
- **PROHIBIDO el uso de `collection.add()`:** Todo documento operativo (`jornadas`, `despachos`, `gastos`) debe generarse con `db.collection(...).document(uuidV4).set(...)`.
- **PROHIBIDO el borrado físico (`delete()`):** Los registros maestros (`catalogos`, `maquinaria`, `usuarios`) utilizan baja lógica (`activo: false` o `estado: false`) para preservar integridad histórica y forense.
- **PROHIBIDO mutar campos operativos desde la web:** El Contador solo tiene autorización para actualizar los campos `estadoValidacion`, `validadoPor`, `fechaValidacion` y `comentarioValidacion`.

### 2.2 En Arquitectura de Código
- **PROHIBIDO acoplar la capa `domain`:**
  - En Android: En `ec.silmay.app.domain` solo se permite Kotlin puro. Prohibido importar `android.*`, `androidx.compose.*` o `com.google.firebase.*`.
  - En Web: En `src/domain` solo se permite TypeScript puro y tipos de negocio.
- **PROHIBIDO el uso de tipos dinámicos o inseguros:**
  - Prohibido el uso de `any` en TypeScript.
  - Prohibido el uso de tipos genéricos sin validación en Kotlin.

### 2.3 En Dependencias y Configuración
- **PROHIBIDO instalar librerías nuevas sin consenso previo:** No agregar dependencias a `build.gradle.kts` o `package.json` que no formen parte de la arquitectura aprobada.
- **PROHIBIDO modificar configuraciones globales del monorepo:** No alterar `tsconfig.json`, reglas de linters, workflows de GitHub Actions o `firestore.rules` sin justificación explícita documentada en una spec.

---

## 3. Checklist Pre-Commit para Desarrolladores (Humano + IA)

Antes de solicitar el merge de cualquier rama:

- [ ] ¿El código compila limpiamente sin warnings ni errores de tipos?
- [ ] ¿Los tests unitarios de la capa de dominio superan el 70% de cobertura?
- [ ] ¿Se verificó que los mensajes mostrados en pantalla provienen de `docs/messages-catalog.md`?
- [ ] ¿Se aseguró que ningún archivo `.env` o credencial privada esté en el `git status`?
- [ ] ¿El formateador automático (`ktlint` o Prettier/Biome) fue ejecutado?