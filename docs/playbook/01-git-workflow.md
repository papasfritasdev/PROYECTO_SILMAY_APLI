# Flujo de Trabajo en Git y Políticas de Integración — SILMAY

Este documento establece las políticas de control de versiones en el monorepo (ADR-0001) para el desarrollo conjunto entre desarrolladores y asistentes de IA (Antigravity y Claude).

---

## 1. Estructura y Jerarquía de Ramas

```text
main (Producción - Despliegues estables)
▲
│ Pull Request con aprobación formal y CI en verde
develop (Integración y Staging de Sprints)
▲
│ Pull Request por funcionalidad
feature/sp[N]-[modulo]-[descripcion-corta]
```

*(Jerarquía de ramas para el monorepo SILMAY).*

- **`main`:** Código de producción validado. Protegida contra pushes directos. Solo recibe merges desde `develop` al finalizar hitos o sprints mayores mediante PR revisado por ambos desarrolladores.
- **`develop`:** Rama de integración continua activa. Todo el trabajo del sprint en curso converge aquí. Protegida contra pushes directos.
- **`feature/*`:** Ramas de trabajo atómicas. Se crean desde `develop` y se reintegran a `develop`. Ejemplo: `feature/sp1-base-android`, `feature/sp1-login-movil`.

---

## 2. Convención de Commits Semánticos (Conventional Commits)

Toda confirmación generada por el desarrollador o sugerida por la IA debe seguir el formato estándar:

```text
<tipo>(<alcance>): <descripción corta en imperativo>
```

### Tipos Permitidos
- `feat`: Nueva funcionalidad de usuario o módulo.
- `fix`: Corrección de un defecto o bug.
- `refactor`: Modificación de código que no altera el comportamiento funcional.
- `test`: Adición o ajuste de pruebas unitarias o de integración.
- `docs`: Cambios en la carpeta `/docs` o documentación interna.
- `chore`: Mantenimiento de build, Gradle, npm o GitHub Actions.

### Alcances (`<alcance>`) Obligatorios
- `android`: Módulo nativo Kotlin/Compose (`ec.silmay.app`).
- `web`: Plataforma React 18 / Vite.
- `functions`: Cloud Functions en backend Node.js.
- `rules`: Security Rules o índices compuestos de Firestore.
- `docs`: Documentos de arquitectura y especificaciones.

**Ejemplo:** `feat(android): implementar pantalla de login con soporte offline y MSG-01`

---

## 3. Protocolo de Pull Requests (PR) y Code Review Humano

Queda estrictamente prohibido que un commit entre a `develop` o `main` sin pasar por un Pull Request.

### Checklist Obligatorio del Pull Request
Antes de solicitar la aprobación del compañero:

- [ ] **Compilación Limpia:** El subproyecto afectado (`android`, `web` o `functions`) compila localmente con cero errores.
- [ ] **Cobertura ≥70% en Dominio:** La suite de pruebas unitarias pasa al 100% y supera el umbral de cobertura exigido en el DoD (RNF-14).
- [ ] **Clean Architecture Respetada:** Se verificó manualmente que la capa `domain` no importe dependencias de Android, Compose, React o Firebase SDK.
- [ ] **Mensajes Canónicos:** Toda alerta visual en la UI utiliza los textos de `docs/messages-catalog.md` (MSG-01 a MSG-16).
- [ ] **Cero Secretos:** No se incluyen claves de servicio, archivos `.env` privados ni `google-services.json` de producción.
- [ ] **Revisión del Diff Humano:** El desarrollador autor del PR comprende cada línea generada por la IA y es capaz de explicarla técnicamente.

---

## 4. Pipeline Automatizado de CI (GitHub Actions)

El archivo `.github/workflows/ci.yml` se ejecuta automáticamente en cada PR hacia `develop`:

- **Job Android:** Ejecuta `./gradlew lintDebug testDebugUnitTest koverHtmlReport` y valida cobertura ≥70%.
- **Job Functions:** Ejecuta `npm run build` y `npm test` en `firebase/functions/`.
- **Job Web:** Ejecuta `npm run build` y `npm run test` en `web/`.