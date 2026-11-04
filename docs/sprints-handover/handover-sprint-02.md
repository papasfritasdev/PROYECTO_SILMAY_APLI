# Informe de Relevo Técnico — Sprint 2: Modelo de Datos, Security Rules, Base Web y Datos Maestros

> **ESTADO: EN PROGRESO (documento vivo).** Se actualiza al cerrar cada historia o PR.
> Última actualización: 04/11/2026 — tras HT-07 (persistencia Android y sync, PR #9).

- **Fecha de Cierre:** — (sprint en curso)
- **Desarrollador Responsable:** Alejandro (`AlejandroR9AlphaCore`)
- **Asistente IA Utilizado:** Claude (Claude Code)
- **Puntos de Historia Completados:** 25 / 27 Pts (HT-05, HT-07, HT-08, HU-01b, HU-02, HU-09)

---

## 1. Historias Implementadas y Estado

| ID | Tipo | Título | Pts | Estado | Enlace a PR |
| :--- | :--- | :--- | :--- | :--- | :--- |
| HT-05 | Arquitectura | Modelo de datos y Security Rules | 5 | Código completo · pendiente merge y CI (Fase 0) | #3 |
| HU-02 | Usuario | Gestión de usuarios y roles | 5 | Código completo (CA1–CA4): backend #5 + UI web #7 | #5, #7 |
| HT-08 | Arquitectura | Base de la plataforma web React | 3 | Código completo · Hosting solo configurado (sin nube) | #6 |
| HU-01b | Usuario | Inicio de sesión por rol en web | 1 | Código completo (CA1–CA3) · pendiente prueba manual | #6 |
| HU-09 | Usuario | Gestión de maquinaria y volquetas | 3 | Código completo (CA1–CA3) · pendiente prueba manual | #8 |
| HT-07 | Arquitectura | Capa de persistencia local y sync | 8 | Código completo (§1–§4) · verificado en Android Emulator + Emulator Suite | #9 |
| HT-12 | Infraestructura | Respaldo mensual de Firestore | 2 | Pendiente (validación real imposible sin nube) | — |

### Deuda del Sprint 1 cerrada durante este sprint (no suma puntos)

| Brecha (auditoría Sprint 1) | Resolución | PR |
| :--- | :--- | :--- |
| Android bloqueaba HTTP al emulador (todo login → MSG-02) | `network_security_config` solo en `src/debug` (10.0.2.2/localhost) | #4 |
| HU-01a CA1: login exitoso no navegaba | `SesionViewModel` + `SilmayNavHost` + `MenuPrincipalScreen` por rol; sesión persistida al reabrir offline | #4 |
| HU-01a CA3: `ERROR_USER_DISABLED` mostraba MSG-01 | Mapeado a MSG-03 en `AuthRepositoryImpl` | #4 |
| Reglas `deny all` impedían leer `usuarios/{uid}` en login | Reglas RBAC de HT-05 | #3 |
| `data_extraction_rules.xml` vacío: en Android 12+ `allowBackup="false"` no impide la transferencia entre dispositivos | Exclusión de todos los dominios en respaldo y transferencia (RNF-05) | #8 |
| `gestionarUsuario` fallaba en el emulador (`admin.firestore.FieldValue` undefined) | Import modular `firebase-admin/firestore` | #7 |

Validación manual P1–P11 (guía en el historial del equipo) **aprobada** por el desarrollador sobre Android Emulator + Firebase Emulator Suite.

### Orden de integración de PRs (apilados)

`#3 (feature/sp2-security-rules → develop)` → `#4 (feature/sp1-cierre-brechas)` → `#5 (feature/sp2-gestion-usuarios)` → `#6 (feature/sp2-web-base)` → `#7 (feature/sp2-usuarios-web)` → `#8 (feature/sp2-maquinaria)` → `#9 (feature/sp2-persistencia-android)`.
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
- **Plataforma Web (`web/src/`, HT-08 + HU-01b):**
  - Dominio (TS puro): `domain/model/Rol.ts`, `UsuarioSesion.ts`, `Resultado.ts`; `domain/constants/messages.ts` (MSG-01..17, verificado contra el `.md`); `domain/usecase/IniciarSesionWebUseCase.ts`, `RestaurarSesionWebUseCase.ts`; `domain/policy/inactividad.ts` (30 min); `domain/navigation/modulosPorRol.ts` (matriz RBAC web); `domain/repository/AuthRepository.ts`
  - Datos: `data/firebase.ts` (emuladores por defecto en dev), `data/repository/FirebaseAuthRepository.ts`, `data/mapper/usuarioMapper.ts`
  - Hooks: `hooks/AuthContext.tsx` (sesión + DI del repositorio), `hooks/useInactividad.ts`
  - Features: `features/auth/LoginPage.tsx`, `features/auth/RutaProtegida.tsx`, `features/panel/PanelLayout.tsx`, `InicioPage.tsx`, `ModuloPendiente.tsx`
  - Config: `web/vite.config.ts` (umbral cobertura 70% en `domain`), `web/vitest.integration.config.ts`; `firebase.json` (bloque `hosting` → `web/dist`, SPA)
  - CI: guardrail de imports prohibidos en `web/src/domain`
- **HU-02 web (`web/src/`):**
  - Dominio: `domain/model/Usuario.ts`, `domain/validation/identidad.ts` (paridad con el servidor), `domain/validation/validarUsuario.ts`, `domain/usecase/GuardarUsuarioUseCase.ts` (alta, edición, baja/reactivación, autoprotección del admin), `domain/repository/UsuariosRepository.ts`
  - Datos: `data/repository/FirebaseUsuariosRepository.ts` (listado `onSnapshot` + callable `gestionarUsuario`), `data/errores.ts` (`already-exists`→MSG-17, permisos→MSG-13, resto→MSG-18)
  - Hooks/Features: `hooks/RepositoriosContext.tsx` (DI), `hooks/useUsuarios.ts`, `features/usuarios/UsuariosPage.tsx`, `features/usuarios/UsuarioFormulario.tsx`
- **HU-09 (`web/src/`):**
  - Dominio: `domain/model/Maquina.ts`, `domain/fleet/flota.ts` (próximo mantenimiento, normalización de código/placa, estado al reactivar), `domain/validation/validarMaquina.ts`, `domain/usecase/GestionarMaquinariaUseCase.ts`, `domain/repository/MaquinariaRepository.ts`
  - Datos: `data/repository/FirebaseMaquinariaRepository.ts` (alta en transacción → MSG-09 sin sobrescribir; placa por consulta), `data/repository/RegistradorRechazos.ts`, `data/mapper/maquinaMapper.ts`
  - Features: `features/maquinaria/MaquinariaPage.tsx`, `MaquinaFormulario.tsx` (próximo mantenimiento calculado en vivo)
  - Pruebas: `src/test/emuladores.ts` (utilidades compartidas de integración)
- **Functions:** `firebase/functions/src/registrarRechazo.ts` (callable, autor desde el token)
- **Android:** `res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml` (exclusión total)
- **HT-07 (`android/app/src/main/java/ec/silmay/app/`):**
  - Dominio (Kotlin puro): `domain/model/ColeccionOperativa.kt`, `domain/model/MetadatosControl.kt` (6 campos de control, claves reservadas), `domain/sync/UuidV4.kt` (validación + `GeneradorUuid`/`Reloj`), `domain/sync/ResolucionPendiente.kt`, `domain/usecase/GuardarRegistroOperativoUseCase.kt`, contratos `RegistroOperativoRepository`, `DispositivoRepository`, `AuditoriaRechazosRepository`
  - Datos: `data/repository/FirestoreRegistroOperativoRepository.kt` (`document(uuid).set()` + confirmación local por lectura de caché, MSG-04 ante disco lleno), `data/sync/BitacoraPendientes.kt`, `data/sync/ConciliadorSincronizacion.kt`, `data/repository/FunctionsAuditoriaRechazosRepository.kt` (`registrarRechazo`, `origen: 'android'`), `data/device/DispositivoRepositoryImpl.kt`, `data/sync/IdentificadoresSistema.kt`
  - DI / arranque: `di/SyncModule.kt` (Functions en `southamerica-east1`, emulador 5001 en debug), `SilmayApp.kt` (inicia el conciliador)
  - Dependencia: `firebase-functions` (BoM existente)
  - Pruebas: 36 unitarias nuevas; instrumentada `src/androidTest/.../data/sync/ColaSincronizacionEmuladorTest.kt`
- **Scripts (`scripts/`):** `sembrar-catalogos.ts` (idempotente con `create()`)
- **Documentación:** `docs/messages-catalog.md` (+ MSG-17, MSG-18), `docs/cloud-functions.md` (§2.2 contrato de `gestionarUsuario`), rango MSG-01..MSG-17 en INDEX/AGENTS/CLAUDE/playbook

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
- [ ] **Cobertura en Dominio:** Web **100%** (Vitest v8, umbral 70% configurado) · Android **NO MEDIDA** (Kover no configurado — deuda Fase 0).
- [x] Clean Architecture: dominios Android y Web sin imports de SDKs/frameworks (guardrail de CI para ambos).
- [x] Catálogo de mensajes verificado: MSG-01..MSG-18 (MSG-17 y MSG-18 agregados por acuerdo; la web verifica su copia contra el `.md` en cada `npm test`).
- [x] Cero credenciales privadas versionadas (`google-services.json` local es un placeholder no versionado).
- [ ] **Pipeline de CI en verde: NO VÁLIDO** — el CI actual omite todos los jobs (deuda Fase 0).

**Resultados locales actuales:**

| Suite | Comando | Resultado |
| :--- | :--- | :--- |
| Android unitarias | `cd android; .\gradlew testDebugUnitTest` | 75/75 |
| Android instrumentadas | `.\gradlew connectedDebugAndroidTest` (Android Emulator + Auth/Firestore/Functions emulator) | 3/3 (HT-07: 50 mutaciones offline + cierre + FIFO; rechazo auditado tras reinicio) |
| Android lint | `.\gradlew lintDebug` | 0 errores · 50 advertencias (ver §5.1) |
| Functions unitarias | `cd firebase\functions; npm test` | 28/28 |
| Functions integración | `npm run test:integration` (Auth + Firestore Emulator) | 13/13 |
| Security Rules | `npm run test:rules` (Firestore Emulator) | 73/73 |
| Web unitarias + componentes | `cd web; npm test` / `npm run coverage` | 110/110 · dominio 100% |
| Web integración | `npm run test:integration` (Auth + Firestore + **Functions** emulator, reglas reales) | 15/15 |

---

## 5. Deuda Técnica y Pendientes

### 5.1 Fase 0 — Bloquea el DoD de TODAS las historias (pospuesta por decisión del equipo)
1. **CI falso verde:** en `.github/workflows/ci.yml` los pasos "Verificar si X está inicializado" comprueban `android/gradlew` y `firebase/functions/package.json` **dentro** de un `working-directory` ya posicionado → siempre `skipped`. Corregir rutas (`./gradlew`, `package.json`).
2. **CI Android sin `google-services.json`:** el plugin falla sin él. Generar un placeholder en el job (no versionado) — el check de secretos prohíbe versionarlo.
3. **Kover:** dependencia aprobada, no configurada. Agregar plugin, regla de verificación ≥ 70% sobre `ec.silmay.app.domain` y `koverHtmlReport` en CI.
4. **CI de Functions incompleto:** agregar `npm run test:rules` y `npm run test:integration` (requieren `firebase-tools` y Java en el runner).
5. **CI web:** el job `web-ci` también tiene el bug de rutas (comprueba `web/package.json` dentro de `working-directory: web`) y no ejecuta `npm test` ni `npm run coverage`.
6. **`lintDebug`** está en el playbook pero no en el CI. Ejecutado localmente: 0 errores; 50 advertencias — 25 `UnusedResources` (los `msg_XX` de `strings.xml` duplican `AppMessage` del dominio y no se usan), 22 de versiones de dependencias (actualizar requiere consenso), 1 `RedundantLabel`, 1 `OldTargetApi`.
7. **PRs apilados sin CI:** el workflow solo corre en PRs hacia `develop`/`main`; los PRs #4–#8 (base = otra rama feature) tienen **0 checks**. Al retargetearse a `develop`, el cambio de base no dispara el workflow: hacer un push o re-ejecutarlo manualmente.

### 5.2 Pendientes de historias del Sprint 2
- **HT-07 — alcance cubierto y límites:**
  - Cubierto: solo **altas** (`create`) de `jornadas`/`despachos`/`gastos`. Las actualizaciones de una jornada abierta (HU-04, Sprint 3) necesitarán un método `actualizar` que conserve `uuid`/`createdAtClient` y no reescriba campos inmutables; la conciliación de rechazos de `update` no está implementada.
  - El cierre forzado se prueba con `FirebaseFirestore.terminate()` + instancia nueva (misma base SQLite en disco). Falta una verificación manual con `adb shell am force-stop` y reinicio del dispositivo cuando exista una pantalla de captura (Sprint 3).
  - La cola FIFO, el backoff y la persistencia de las mutaciones son del SDK de Firestore (ADR-0004); la app solo agrega la bitácora de verificación. Una entrada se pierde (sin auditoría) si la app muere entre la escritura local y el `commit()` de la bitácora: ventana de milisegundos, de mejor esfuerzo.
  - Aún no hay consumidores: las pantallas de captura llegan en el Sprint 3 y deben usar `GuardarRegistroOperativoUseCase`.
- **HT-12:** sin nube, la exportación/restauración solo puede probarse con mocks; documentar la limitación.

### 5.3 Decisiones abiertas con el equipo
- ~~Mensaje para `failed-precondition`~~ → resuelto: la UI deshabilita baja y cambio de rol del propio admin (nunca se dispara).
- ~~Mensaje para fallo de red en login web~~ → resuelto con **MSG-18** (genérico, acordado).
- **Validación de formularios sin mensajes de catálogo** (acordado): ayudas de formato fijas por campo (`aria-describedby`), marcado `aria-invalid` y botón Guardar bloqueado.
- Vacíos de esquema: PK de `conciliaciones` (`periodo`) colisiona entre `tipo` despachos/gastos; HU-11 pide "fecha de atención" no definida en `notificaciones`; `telefono` (solo Admin) no es restringible por campo en reglas.
- Bloqueo de cierre de sesión con registros pendientes de sync (sync-protocol §5) → Sprint 3 (HU-08).
- **Seguridad de scripts:** `scripts/crear-admin-inicial.ts` en modo `--prod` usa la contraseña por defecto `Silmay2026!Admin` (publicada en el repo) si no se define `ADMIN_PASSWORD`. Antes de cualquier uso en la nube: exigir la variable y fallar si falta.
- **Atomicidad de `gestionarUsuario` (limitación):** al editar se actualiza Auth y luego Firestore; si Firestore falla, ambos quedan desalineados (no hay transacciones entre Auth y Firestore).
- **Unicidad de placa (limitación):** se verifica por consulta previa; el SDK web no admite consultas dentro de transacciones, así que dos altas simultáneas con la misma placa podrían pasar (riesgo bajo: solo el admin registra flota).
- **`registrarRechazo` sin límite de frecuencia:** un usuario autenticado podría inflar la auditoría con entradas a su propio nombre.

### 5.4 Advertencias y "Gotchas"
- **Solo emulador** (decisión por costos): no hay proyecto en la nube; HT-02 (presupuesto) y el despliegue de Hosting no son verificables.
- **Entorno Windows:** `JAVA_HOME` debe apuntar a un JDK existente (p. ej. `C:\Program Files\Android\Android Studio\jbr`); `android/local.properties` con `sdk.dir`; `android/app/google-services.json` placeholder (project_id `silmay-dev`) basta para compilar y usar emuladores.
- **`@firebase/rules-unit-testing` 5.x** (no 6.x): la 6 exige Node ≥ 24; CI y runtime usan Node 20.
- Las pruebas de reglas/integración **borran los datos** del emulador y necesitan los puertos libres (cerrar `firebase emulators:start` antes).
- `10.0.2.2` solo funciona en Android Emulator, no en dispositivo físico.
- **`google-services.json` local:** el SDK de Functions valida el **formato** de la API key (`AIza` + 35 caracteres) antes de llamar, incluso contra el emulador. El placeholder anterior (`PLACEHOLDER…`) hacía fallar en silencio `registrarRechazo` desde Android; se cambió por una clave ficticia con formato válido (archivo no versionado, acordado con el desarrollador). Cualquier placeholder del equipo debe respetar ese formato.
- **Pruebas instrumentadas de HT-07:** requieren `firebase emulators:start --only auth,firestore,functions` (tras `npm run build` en `firebase/functions`) y un AVD encendido; crean un operador nuevo por ejecución vía REST del emulador (`Bearer owner`). En la primera ejecución sobre un AVD recién arrancado la instrumentación terminó una vez con "Process crashed" sin traza en logcat; no se reprodujo en 4 ejecuciones posteriores.
- **Bug corregido (estaba desde el Sprint 1):** el emulador de Functions envuelve `firebase-admin` en un proxy donde `admin.firestore.FieldValue` es `undefined`; `gestionarUsuario` fallaba con `internal` en el emulador (el alta se revertía y la edición dejaba Auth actualizado sin Firestore). Usar siempre `import { FieldValue } from "firebase-admin/firestore"`.
- **Lección de pruebas:** las pruebas de integración de Functions (`npm run test:integration` en `firebase/functions`) ejecutan el TypeScript con Vitest y **no** pasan por el runtime del emulador; por eso no detectaron el bug anterior. La suite de integración web sí invoca la función real en el emulador y debe ejecutarse antes de integrar cambios en Functions.
- **Versiones web fijadas por Node 20 (CI):** vite 6, vitest 3, jsdom 26, TypeScript 5.9 (vitest 5 / jsdom 30 exigen Node ≥ 22.22). `react-router-dom` 7 con React 18.
- Dependencias web adicionales aprobadas en Sprint 2: `@types/react`, `@types/react-dom`, `@testing-library/dom` (peer de RTL 16), `@vitest/coverage-v8`.

---

## 6. Objetivo de Arranque Sugerido (siguiente paso)
- **HT-07:** capa de persistencia Android (repositorios con UUID v4 y metadatos de control, cola de 50 mutaciones, auditoría de rechazos).
