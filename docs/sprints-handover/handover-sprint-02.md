# Informe de Relevo Técnico — Sprint 2: Modelo de Datos, Security Rules, Base Web y Datos Maestros

> **ESTADO: EN PROGRESO (documento vivo).** Se actualiza al cerrar cada historia o PR.
> Última actualización: 03/11/2026 — tras HU-02 backend (PR #5).

- **Fecha de Cierre:** — (sprint en curso)
- **Desarrollador Responsable:** Alejandro (`AlejandroR9AlphaCore`)
- **Asistente IA Utilizado:** Claude (Claude Code)
- **Puntos de Historia Completados:** 5 / 27 Pts (HT-05) · 5 Pts más en curso (HU-02, falta UI web)

---

## 1. Historias Implementadas y Estado

| ID | Tipo | Título | Pts | Estado | Enlace a PR |
| :--- | :--- | :--- | :--- | :--- | :--- |
| HT-05 | Arquitectura | Modelo de datos y Security Rules | 5 | Código completo · pendiente merge y CI (Fase 0) | #3 |
| HU-02 | Usuario | Gestión de usuarios y roles | 5 | **Parcial:** backend completo; falta UI web (CA2 "notifica en pantalla") | #5 |
| HT-08 | Arquitectura | Base de la plataforma web React | 3 | Pendiente | — |
| HU-01b | Usuario | Inicio de sesión por rol en web | 1 | Pendiente | — |
| HU-09 | Usuario | Gestión de maquinaria y volquetas | 3 | Pendiente | — |
| HT-07 | Arquitectura | Capa de persistencia local y sync | 8 | Pendiente | — |
| HT-12 | Infraestructura | Respaldo mensual de Firestore | 2 | Pendiente (validación real imposible sin nube) | — |

### Deuda del Sprint 1 cerrada durante este sprint (no suma puntos)

| Brecha (auditoría Sprint 1) | Resolución | PR |
| :--- | :--- | :--- |
| Android bloqueaba HTTP al emulador (todo login → MSG-02) | `network_security_config` solo en `src/debug` (10.0.2.2/localhost) | #4 |
| HU-01a CA1: login exitoso no navegaba | `SesionViewModel` + `SilmayNavHost` + `MenuPrincipalScreen` por rol; sesión persistida al reabrir offline | #4 |
| HU-01a CA3: `ERROR_USER_DISABLED` mostraba MSG-01 | Mapeado a MSG-03 en `AuthRepositoryImpl` | #4 |
| Reglas `deny all` impedían leer `usuarios/{uid}` en login | Reglas RBAC de HT-05 | #3 |

Validación manual P1–P11 (guía en el historial del equipo) **aprobada** por el desarrollador sobre Android Emulator + Firebase Emulator Suite.

### Orden de integración de PRs (apilados)

`#3 (feature/sp2-security-rules → develop)` → `#4 (feature/sp1-cierre-brechas)` → `#5 (feature/sp2-gestion-usuarios)` → rama actual `feature/sp2-web-base`.
Al integrar cada PR y borrar su rama, GitHub retargetea el siguiente automáticamente. **El merge lo realiza el compañero tras su revisión.**

---

## 2. Mapa de Archivos Clave Creados o Modificados

- **Capa Dominio Android (`android/app/src/main/java/ec/silmay/app/domain/`):**
  - Entidades: `model/Rol.kt` (+ `habilitadoEnMovil`), `model/AppMessage.kt` (+ MSG-17)
  - Casos de Uso: `usecase/ObtenerSesionActualUseCase.kt`, `usecase/CerrarSesionUseCase.kt`, `usecase/IniciarSesionUseCase.kt` (refactor RBAC)
- **Capa Datos Android (`data/`):**
  - `repository/AuthRepositoryImpl.kt`: MSG-03 ante `ERROR_USER_DISABLED`; sesión offline con rol del perfil cacheado
- **Capa Presentación Android (`presentation/`):**
  - `sesion/SesionViewModel.kt`, `navigation/SilmayNavHost.kt`, `menu/MenuPrincipalScreen.kt`; `MainActivity.kt` usa el NavHost
  - Debug: `android/app/src/debug/AndroidManifest.xml`, `android/app/src/debug/res/xml/network_security_config.xml`
- **Backend e Infraestructura (`firebase/`):**
  - Rules / Indexes: `firebase/firestore.rules` (12 colecciones), `firebase/firestore.indexes.json` (5 índices)
  - Functions: `firebase/functions/src/gestionarUsuario.ts` (HU-02 CA1–CA4), `src/utils/validaciones.ts` (+ contraseña y teléfono)
  - Pruebas: `firebase/functions/test/rules/firestore.rules.test.ts`, `test/integration/gestionarUsuario.integration.test.ts`
  - Configs: `vitest.rules.config.ts`, `vitest.integration.config.ts`
- **Scripts (`scripts/`):** `sembrar-catalogos.ts` (idempotente con `create()`)
- **Documentación:** `docs/messages-catalog.md` (+ MSG-17), `docs/cloud-functions.md` (§2.2 contrato de `gestionarUsuario`), rango MSG-01..MSG-17 en INDEX/AGENTS/CLAUDE/playbook

---

## 3. Contratos de Datos y Esquema Firestore

- **Security Rules (resumen):** RBAC por claim `role` + baja lógica inmediata (`usuarios.estado`); creación operativa solo por titular móvil con `uuid == docId`, `zona == zonaAsignada`, `syncedAtServer == request.time`; jornadas cerradas inmutables; reintento idempotente permitido si solo cambia `syncedAtServer`; contador limitado a los 4 campos de validación; `maquinaria` con ID = `codigoInterno`; `allow delete: if false` global.
- **`gestionarUsuario`:** ver `docs/cloud-functions.md` §2.2 (errores → `MSG-13`, `MSG-17`).
- **Catálogos sembrados (emulador):** materiales (4), rubros (4), motivos (6), `parametros/sistema` (umbral 0.90, `America/Guayaquil`); lugares (3) y clientes (2) **de ejemplo** solo con `--local`.
- **Cuentas de prueba (emulador, se recrean en cada sesión):**
  - `admin@silmay.ec` / `Silmay2026!Admin` (`role: admin`) → `npm run crear-admin:local`
  - `operador@silmay.ec` / `Silmay2026!Operador` (`role: operador`) → `npx tsx crear-operador-prueba.ts --local`

---

## 4. Auditoría de Calidad y Definición de Terminado (DoD)

- [x] Criterios Gherkin cubiertos por pruebas en lo implementado (HT-05, HU-02 backend, HU-01a CA1/CA3).
- [ ] **Cobertura de pruebas unitarias en Dominio: NO MEDIDA** (Kover no configurado — deuda Fase 0).
- [x] Clean Architecture: dominio Android sin imports de SDKs.
- [x] Catálogo de mensajes verificado: MSG-01..MSG-17 (MSG-17 agregado por acuerdo).
- [x] Cero credenciales privadas versionadas (`google-services.json` local es un placeholder no versionado).
- [ ] **Pipeline de CI en verde: NO VÁLIDO** — el CI actual omite todos los jobs (deuda Fase 0).

**Resultados locales actuales:**

| Suite | Comando | Resultado |
| :--- | :--- | :--- |
| Android unitarias | `cd android; .\gradlew testDebugUnitTest` | 39/39 |
| Functions unitarias | `cd firebase\functions; npm test` | 24/24 |
| Functions integración | `npm run test:integration` (Auth + Firestore Emulator) | 11/11 |
| Security Rules | `npm run test:rules` (Firestore Emulator) | 73/73 |

---

## 5. Deuda Técnica y Pendientes

### 5.1 Fase 0 — Bloquea el DoD de TODAS las historias (pospuesta por decisión del equipo)
1. **CI falso verde:** en `.github/workflows/ci.yml` los pasos "Verificar si X está inicializado" comprueban `android/gradlew` y `firebase/functions/package.json` **dentro** de un `working-directory` ya posicionado → siempre `skipped`. Corregir rutas (`./gradlew`, `package.json`).
2. **CI Android sin `google-services.json`:** el plugin falla sin él. Generar un placeholder en el job (no versionado) — el check de secretos prohíbe versionarlo.
3. **Kover:** dependencia aprobada, no configurada. Agregar plugin, regla de verificación ≥ 70% sobre `ec.silmay.app.domain` y `koverHtmlReport` en CI.
4. **CI de Functions incompleto:** agregar `npm run test:rules` y `npm run test:integration` (requieren `firebase-tools` y Java en el runner).
5. **Cobertura web:** Vitest necesita `@vitest/coverage-v8` (pendiente de aprobación) para el DoD de la web.
6. **`lintDebug`** está en el playbook pero no en el CI.

### 5.2 Pendientes de historias del Sprint 2
- **HU-02 web:** pantallas de alta/edición/baja; mapear `already-exists` → MSG-17.
- **HU-09:** alta de maquinaria con **transacción** (leer → MSG-09 si existe → crear): un `set()` sobre ID existente es *update* y las reglas no lo impiden; verificación de placa por consulta.
- **HT-07 / HU-09 CA3:** callable `registrarRechazo` (aprobada) para auditar `PERMISSION_DENIED`; documentarla en `docs/cloud-functions.md`.
- **HT-12:** sin nube, la exportación/restauración solo puede probarse con mocks; documentar la limitación.

### 5.3 Decisiones abiertas con el equipo
- Mensaje de catálogo para `failed-precondition` (admin intenta darse de baja o retirarse su rol).
- Vacíos de esquema: PK de `conciliaciones` (`periodo`) colisiona entre `tipo` despachos/gastos; HU-11 pide "fecha de atención" no definida en `notificaciones`; `telefono` (solo Admin) no es restringible por campo en reglas.
- Bloqueo de cierre de sesión con registros pendientes de sync (sync-protocol §5) → Sprint 3 (HU-08).

### 5.4 Advertencias y "Gotchas"
- **Solo emulador** (decisión por costos): no hay proyecto en la nube; HT-02 (presupuesto) y el despliegue de Hosting no son verificables.
- **Entorno Windows:** `JAVA_HOME` debe apuntar a un JDK existente (p. ej. `C:\Program Files\Android\Android Studio\jbr`); `android/local.properties` con `sdk.dir`; `android/app/google-services.json` placeholder (project_id `silmay-dev`) basta para compilar y usar emuladores.
- **`@firebase/rules-unit-testing` 5.x** (no 6.x): la 6 exige Node ≥ 24; CI y runtime usan Node 20.
- Las pruebas de reglas/integración **borran los datos** del emulador y necesitan los puertos libres (cerrar `firebase emulators:start` antes).
- `10.0.2.2` solo funciona en Android Emulator, no en dispositivo físico.

---

## 6. Objetivo de Arranque Sugerido (siguiente paso)
- **HT-08 + HU-01b:** base web React 18 + Vite + TS (Clean Architecture), login contador/admin, guards por rol, cierre por inactividad de 30 min.
