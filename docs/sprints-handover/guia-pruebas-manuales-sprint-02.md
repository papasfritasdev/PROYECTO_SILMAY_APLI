# Guía de Pruebas Manuales — Sprint 2

> Valida a mano todo lo desarrollado en el Sprint 2 (PRs #3–#10) sobre **Firebase Emulator Suite**
> (sin nube). Cada prueba indica el resultado esperado y dónde está el código que la implementa.
> Rama a usar: `feature/sp2-respaldo-firestore` (la última de la pila; contiene todo).
> Las referencias `archivo:línea` corresponden a esa rama al 04/11/2026.

Marca cada casilla al completar. Si algo falla, anota el ID de la prueba (p. ej. `W-07`).

---

## 0. Preparación del entorno (una vez por sesión)

Abre **4 terminales PowerShell** en la raíz del repo (`C:\Users\mcnek\PROYECTO_SILMAY_APLI`).

| Paso | Terminal | Comando | Resultado esperado |
| :--- | :--- | :--- | :--- |
| 0.1 | T1 | `git checkout feature/sp2-respaldo-firestore; git pull` | Rama actualizada |
| 0.2 | T1 | `npm --prefix firebase/functions run build` | Compila sin errores |
| 0.3 | T1 | `firebase emulators:start --only auth,firestore,functions --project silmay-dev` | "All emulators ready!" y `Loaded functions definitions from source: gestionarUsuario, registrarRechazo, respaldoMensual` |
| 0.4 | T2 | `cd scripts; npm run crear-admin:local` | Crea `admin@silmay.ec` / `Silmay2026!Admin` |
| 0.5 | T2 | `npx tsx crear-operador-prueba.ts --local` | Crea `operador@silmay.ec` / `Silmay2026!Operador` (zona `cantera-banos`) |
| 0.6 | T2 | `npm run sembrar-catalogos:local` | Siembra materiales, rubros, motivos, `parametros/sistema` y lugares/clientes de ejemplo |
| 0.7 | T3 | `cd web; npm run dev` | Vite en `http://localhost:5173` |
| 0.8 | — | Android Studio → abrir `android/` → AVD **Pixel_7** → Run `app` (variante *debug*) | App instalada en el emulador |

- **Emulator UI:** `http://127.0.0.1:4000` (pestañas *Authentication*, *Firestore*, *Logs*). Aquí verificarás perfiles, `auditoria` y registros.
- Los emuladores **no guardan datos** entre sesiones: si los reinicias, repite 0.4–0.6.
- Si `JAVA_HOME` falla en Gradle: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`.

Cédulas válidas de prueba (Módulo 10) que puedes usar en los formularios: `1704567898`, `0903123453`, `1109876548`, `0105551238`.
(Ya usadas: `1710034065` admin, `0922879580` operador.)

---

## 1. HT-05 — Security Rules, índices y catálogos (PR #3)

| ID | Prueba | Esperado | Código |
| :--- | :--- | :--- | :--- |
| ☐ R-01 | Detén T1 (`Ctrl+C`). En T1: `cd firebase/functions; npm run test:rules` | **73/73** en verde | `firebase/functions/test/rules/firestore.rules.test.ts` |
| ☐ R-02 | Relanza el paso 0.3 (y 0.4–0.6). En Emulator UI → Firestore, revisa `catalogos/materiales/items` | 4 materiales (Arena normal, Arena fina, Piedra, Ripio) | `scripts/sembrar-catalogos.ts:44` |
| ☐ R-03 | Vuelve a ejecutar `npm run sembrar-catalogos:local` | Informa existentes, **no** duplica ni sobrescribe | `scripts/sembrar-catalogos.ts:83` (`create()`) |
| ☐ R-04 | Abre `firebase/firestore.rules` y verifica | Baja lógica `isActive` (l. 34), campos de control al crear (l. 101), reintento idempotente (l. 112), contador solo 4 campos (l. 140), `maquinaria` (l. 328), `auditoria` (l. 470), `deny all` final (l. 476), `allow delete: if false` en todas | `firebase/firestore.rules` |
| ☐ R-05 | Emulator UI → Firestore → pestaña **Requests** durante las pruebas web/Android | Cada lectura/escritura muestra la regla evaluada (ALLOW/DENY) | — |

> R-01 borra los datos del emulador; por eso se relanza después.

---

## 2. Android — HU-01a (brechas del Sprint 1, PR #4)

Ya los validaste como P1–P11; repítelos como regresión rápida.

| ID | Prueba | Esperado | Código |
| :--- | :--- | :--- | :--- |
| ☐ A-01 | Login con `operador@silmay.ec` / `Silmay2026!Operador` | Entra al **menú principal de operador** | `presentation/navigation/SilmayNavHost.kt:59-66`, `presentation/menu/MenuPrincipalScreen.kt:30` |
| ☐ A-02 | Cierra la app (deslizar), activa **modo avión** en el AVD, ábrela | Entra directo al menú sin red (sesión persistida) | `domain/usecase/ObtenerSesionActualUseCase.kt`, `data/repository/AuthRepositoryImpl.kt` (`obtenerSesionActual`, `Source.CACHE`) |
| ☐ A-03 | Cierra sesión. Con modo avión, intenta login | **MSG-02** "Se requiere conexión a Internet…" | `data/repository/AuthRepositoryImpl.kt:63` |
| ☐ A-04 | Quita modo avión. Login con contraseña incorrecta | **MSG-01** (mensaje opaco) | `domain/usecase/IniciarSesionUseCase.kt:26` |
| ☐ A-05 | Login con `admin@silmay.ec` en el móvil | **MSG-13** "Esta plataforma no está habilitada para su rol." | `domain/usecase/IniciarSesionUseCase.kt:41-43`, `domain/model/Rol.kt` (`habilitadoEnMovil`) |
| ☐ A-06 | Login de un usuario dado de baja (se prepara en W-12) | **MSG-03** "Cuenta deshabilitada…" | `data/repository/AuthRepositoryImpl.kt:67-68` |
| ☐ A-07 | Supervisor (crearlo en W-08) inicia sesión | Menú de supervisor con "registros de mi zona" | `presentation/menu/MenuPrincipalScreen.kt:34-39` |

Rutas Android relativas a `android/app/src/main/java/ec/silmay/app/`.

---

## 3. Web — HT-08 + HU-01b (base web y login por rol, PR #6)

Abre `http://localhost:5173`.

| ID | Prueba | Esperado | Código |
| :--- | :--- | :--- | :--- |
| ☐ W-01 | Entra a `http://localhost:5173/usuarios` sin sesión | Redirige a `/login` | `web/src/features/auth/RutaProtegida.tsx:28` |
| ☐ W-02 | Login `admin@silmay.ec` / `Silmay2026!Admin` | Panel con módulos de admin: Monitoreo, Reportería, Alertas, **Usuarios**, **Maquinaria**, Catálogos | `web/src/domain/navigation/modulosPorRol.ts:22`, `web/src/App.tsx:22-44` |
| ☐ W-03 | Cierra sesión. Login con `operador@silmay.ec` | **MSG-13** y la sesión se cierra (no entra) | `web/src/domain/usecase/IniciarSesionWebUseCase.ts:37-39` |
| ☐ W-04 | Login con contraseña incorrecta | **MSG-01** | `web/src/data/repository/FirebaseAuthRepository.ts:21` |
| ☐ W-05 | Detén el emulador (T1) e intenta login | **MSG-18** "No se pudo completar la operación…" (fallo de red ≠ credenciales). Relanza 0.3–0.6 después | `web/src/data/repository/FirebaseAuthRepository.ts:19` |
| ☐ W-06 | Como admin, recarga la página (F5) | Sigue autenticado en el panel | `web/src/hooks/AuthContext.tsx`, `web/src/domain/usecase/RestaurarSesionWebUseCase.ts` |
| ☐ W-07 | **Inactividad (RNF-07):** logueado, no toques teclado ni ratón **30 min** | Cierra sesión sola y vuelve a `/login`. (Mover el ratón antes reinicia el contador) | `web/src/domain/policy/inactividad.ts:6`, `web/src/hooks/useInactividad.ts:5-40`, `web/src/hooks/AuthContext.tsx:77` |

> W-07 tarda 30 minutos; déjalo corriendo en otra ventana mientras haces lo demás (sin interactuar con esa pestaña, pero ten en cuenta que los eventos se escuchan en la ventana).

---

## 4. Web — HU-02 (gestión de usuarios y roles, PRs #5 y #7)

Como **admin** → módulo **Usuarios**.

| ID | Prueba | Esperado | Código |
| :--- | :--- | :--- | :--- |
| ☐ W-08 | **CA1 alta:** "Nuevo usuario" → Nombres `Contadora Prueba`, Cédula `1704567898`, Teléfono `0991234567`, Correo `contador@silmay.ec`, Contraseña `Conta2026x`, Zona `cantera-banos`, Cargo `Contadora`, Rol **contador** → Guardar | Aparece en la tabla con estado activo. Repite con `supervisor@silmay.ec` (cédula `0903123453`, rol **supervisor**) | UI `web/src/features/usuarios/UsuarioFormulario.tsx:21-38`; backend `firebase/functions/src/gestionarUsuario.ts:188-192` (Auth + Custom Claim `role`) |
| ☐ W-09 | Emulator UI → Authentication → `contador@silmay.ec` → *Custom claims* | `{"role":"contador"}`; en Firestore `usuarios/{uid}` con `estado: true` | `firebase/functions/src/gestionarUsuario.ts:192` |
| ☐ W-10 | **Validaciones:** cédula `1234567890`, teléfono `123`, contraseña `abc` | Campos marcados en rojo con la ayuda de formato; **Guardar deshabilitado** (no hay mensajes fuera del catálogo, decisión del equipo) | `web/src/domain/validation/validarUsuario.ts:28-37`, `web/src/domain/validation/identidad.ts:8`, `UsuarioFormulario.tsx:62,141` |
| ☐ W-11 | **CA2 duplicados:** nuevo usuario con correo `contador@silmay.ec` (otra cédula válida `1109876548`); luego otro con cédula `1704567898` y correo nuevo | **MSG-17** "Ya existe un usuario registrado con ese correo o cédula." en ambos | `firebase/functions/src/gestionarUsuario.ts:139,325`; `web/src/data/errores.ts:15` |
| ☐ W-12 | **CA3 baja lógica:** "Dar de baja" al operador | Estado inactivo. En Android login con operador → **MSG-03** (A-06). En web Authentication el usuario aparece *disabled* | `gestionarUsuario.ts:235,249` (`disabled` + `revokeRefreshTokens`); `web/src/domain/usecase/GuardarUsuarioUseCase.ts:45` |
| ☐ W-13 | "Reactivar" al operador | Vuelve a poder entrar en Android | `GuardarUsuarioUseCase.ts:45` |
| ☐ W-14 | **Autoprotección:** en tu propia fila (admin) | "Dar de baja" deshabilitado (tooltip) y el selector de rol bloqueado al editarte | `web/src/features/usuarios/UsuariosPage.tsx:123-130`, `UsuarioFormulario.tsx:116`, `GuardarUsuarioUseCase.ts:18` |
| ☐ W-15 | **Editar** al supervisor: cambia zona a `cantera-norte`, deja contraseña vacía | Se guarda; la contraseña anterior sigue funcionando | `UsuarioFormulario.tsx` (ayuda "Opcional"), `gestionarUsuario.ts:235-242` |
| ☐ W-16 | **CA4 auditoría:** Emulator UI → Firestore → `auditoria` | Un documento por alta/edición/baja con `accion` `CREATE`/`UPDATE`, `usuarioId` = UID del admin y `fecha` | `firebase/functions/src/gestionarUsuario.ts:152-165` |
| ☐ W-17 | Login web como `contador@silmay.ec` | Panel solo con Monitoreo, Validación, Reportería, Alertas; **sin** Usuarios ni Maquinaria. Entrar a `/usuarios` a mano redirige a `/` | `modulosPorRol.ts:23`, `RutaProtegida.tsx:31-32` |

---

## 5. Web — HU-09 (maquinaria y volquetas, PR #8)

Como **admin** → módulo **Maquinaria**.

| ID | Prueba | Esperado | Código |
| :--- | :--- | :--- | :--- |
| ☐ W-18 | **CA1:** "Nueva unidad" → Código `EX-01`, Placa vacía, Marca `CAT`, Modelo `320`, Tipo **excavadora**, Horómetro base `1200.5`, Intervalo `250` | Mientras escribes muestra próximo mantenimiento **1450.5 h**. Guardado con estado `operativa` | `web/src/domain/fleet/flota.ts:9`, `web/src/features/maquinaria/MaquinaFormulario.tsx:20-25`; Firestore `maquinaria/EX-01` |
| ☐ W-19 | Nueva unidad tipo **volqueta** `VOL-01` **sin placa** | Guardar deshabilitado (placa obligatoria en volquetas). Con placa `ABC-1234` se guarda | `web/src/domain/validation/validarMaquina.ts:31-32` |
| ☐ W-20 | **CA2 código duplicado:** nueva unidad con código `ex-01` (minúsculas) | **MSG-09** "Ya existe una unidad de maquinaria registrada con ese código/placa." y `EX-01` **no** se modifica | `flota.ts:14` (normaliza), `web/src/data/repository/FirebaseMaquinariaRepository.ts:61-67` (transacción) |
| ☐ W-21 | **CA2 placa duplicada:** `VOL-02` con placa `abc1234` | **MSG-09** | `flota.ts:22`, `FirebaseMaquinariaRepository.ts:55-56,89-91` |
| ☐ W-22 | Editar `EX-01`: cambia marca/modelo | Código, horómetro base e intervalo **no** son editables | `MaquinaFormulario.tsx:20-25` (`editable: false`) |
| ☐ W-23 | "Dar de baja" a `VOL-01` y luego "Reactivar" | Pasa a `inactiva`; al reactivar vuelve a `operativa` (o `mantenimiento` si su horómetro alcanzó el próximo mantenimiento) | `MaquinariaPage.tsx:113-119`, `flota.ts:32-33` |
| ☐ W-24 | **CA3 protección en servidor + auditoría.** Necesitas 2 admins: crea `admin2@silmay.ec` (rol admin, cédula `0105551238`). Abre una **ventana de incógnito** con `admin2` → Maquinaria → "Nueva unidad", rellena `EX-02` **sin guardar**. En la ventana normal (admin) da de **baja** a `admin2`. Vuelve a incógnito y pulsa Guardar | **MSG-13**; la unidad **no** se crea; en `auditoria` aparece `accion: PERMISSION_DENIED`, `coleccion: maquinaria`, `documentoId: EX-02`, `detalle.origen: web`, `usuarioId` = UID de admin2 | Reglas `firebase/firestore.rules:328-345`; `FirebaseMaquinariaRepository.ts:96-104`; `web/src/data/repository/RegistradorRechazos.ts:18-22`; callable `firebase/functions/src/registrarRechazo.ts` |
| ☐ W-25 | Login como contador → intenta `/maquinaria` | Redirige a `/` (ni siquiera ve el módulo) | `modulosPorRol.ts:23` |

> W-24: las reglas leen `usuarios/{uid}.estado` en cada petición, así que la baja surte efecto
> en la siguiente escritura aunque el token siga vigente. Si la ventana de incógnito te saca a
> `/login` antes de guardar, repite pulsando Guardar más rápido tras la baja.

---

## 6. Android — HT-07 (persistencia local y sync, PR #9)

HT-07 es infraestructura: **todavía no tiene pantalla** (las capturas llegan en el Sprint 3).
Se prueba con la batería instrumentada contra los emuladores, observando los efectos en la Emulator UI.

| ID | Prueba | Esperado | Código |
| :--- | :--- | :--- | :--- |
| ☐ S-01 | Con emuladores (0.3) y AVD encendidos: Android Studio → abre `android/app/src/androidTest/java/ec/silmay/app/data/sync/ColaSincronizacionEmuladorTest.kt` → ▶ junto a la clase. (Alternativa T4: `cd android; .\gradlew connectedDebugAndroidTest`) | **2/2** en verde (≈ 30 s) | Test completo |
| ☐ S-02 | Emulator UI → Authentication | Usuarios nuevos `op-ht07-xxxx@silmay.ec` (uno por prueba) | Test l. preparación |
| ☐ S-03 | Emulator UI → Firestore → `gastos` | **50** documentos de la prueba FIFO, cada uno con `uuid` = ID del documento, `userId`, `deviceId`, `zona: cantera-banos`, `createdAtClient` y `syncedAtServer` | Metadatos: `domain/usecase/GuardarRegistroOperativoUseCase.kt:31-62`; escritura `data/repository/FirestoreRegistroOperativoRepository.kt:48-55` |
| ☐ S-04 | Ordena mentalmente por `montoUSD` 1…50 y compara `syncedAtServer` | Crecen en el mismo orden (FIFO tras "cierre" de la app) | Cola del SDK (ADR-0004); `docs/sync-protocol.md` §3.5 |
| ☐ S-05 | Firestore → `auditoria` | Una entrada `PERMISSION_DENIED`, `coleccion: gastos`, `detalle.origen: android`, `detalle.operacion: create` (el gasto con rubro inválido, rechazado tras reiniciar) | `data/sync/ConciliadorSincronizacion.kt:91-116`; `data/repository/FunctionsAuditoriaRechazosRepository.kt:23-38` |
| ☐ S-06 | En `gastos`, busca un documento con `rubro: rubro-inexistente` | **No existe** (las reglas lo rechazaron y el SDK lo revirtió) | Reglas `firebase/firestore.rules:279` |
| ☐ S-07 | Unitarias: T4 `cd android; .\gradlew testDebugUnitTest` | **75/75** | `android/app/src/test/...` |
| ☐ S-08 | Revisa el arranque | `SilmayApp` inicia el conciliador; DI en `di/SyncModule.kt` (Functions en `southamerica-east1`, emulador 5001) | `SilmayApp.kt:17`, `di/SyncModule.kt` |

Otros puntos de HT-07 para revisar en código:
- Claves de control enviadas por el llamador se descartan: `GuardarRegistroOperativoUseCase.kt:62`.
- Disco lleno → MSG-04: `FirestoreRegistroOperativoRepository.kt` (`mensajePara`).
- `deviceId` UUID persistente por instalación: `data/device/DispositivoRepositoryImpl.kt:30-32`.
- Backoff de reintento 30 s → 15 min: `ConciliadorSincronizacion.kt:41-42,54-70`.

---

## 7. Android — RNF-05 respaldo deshabilitado (PR #8)

| ID | Prueba | Esperado | Código |
| :--- | :--- | :--- | :--- |
| ☐ B-01 | Revisa el manifiesto | `android:allowBackup="false"` + `dataExtractionRules` + `fullBackupContent` | `android/app/src/main/AndroidManifest.xml:10-12` |
| ☐ B-02 | T4: `adb shell bmgr backupnow ec.silmay.app` | El sistema informa que el paquete no admite respaldo / no se respalda nada | `android/app/src/main/res/xml/data_extraction_rules.xml` (excluye todos los dominios en `cloud-backup` y `device-transfer`) |
| ☐ B-03 | Revisa la config de red de debug | Texto plano permitido **solo** hacia `10.0.2.2`, `localhost`, `127.0.0.1`; en *release* no existe | `android/app/src/debug/res/xml/network_security_config.xml:9-13` |

---

## 8. HT-12 — Respaldo mensual (PR #10)

No ejecutable de verdad sin nube; se valida la lógica y la configuración.

| ID | Prueba | Esperado | Código |
| :--- | :--- | :--- | :--- |
| ☐ H-01 | T4: `cd firebase/functions; npm test` | **35/35** (incluye 7 de `respaldoMensual`) | `firebase/functions/src/respaldoMensual.test.ts` |
| ☐ H-02 | En la salida de T1 (paso 0.3) | `respaldoMensual` cargada e "ignored because the pubsub emulator does not exist" (esperado) | — |
| ☐ H-03 | Lee `respaldoMensual.ts` | Cron `0 3 1 * *` en `America/Guayaquil`, región `southamerica-east1`, 3 reintentos, exporta todas las colecciones a `gs://<bucket>/firestore/AAAA-MM-DDTHH-mm` | `firebase/functions/src/respaldoMensual.ts` (`CRON_RESPALDO`, `construirSolicitud`, `respaldoMensual`) |
| ☐ H-04 | Lee el runbook | Pasos de bucket/IAM, verificación, restauración en `silmay-dev` y checklist de validación | `docs/runbooks/restauracion-firestore.md` |

---

## 9. Suites automáticas (resumen)

| Suite | Comando | Esperado |
| :--- | :--- | :--- |
| Android unitarias | `cd android; .\gradlew testDebugUnitTest` | 75/75 |
| Android instrumentadas | `.\gradlew connectedDebugAndroidTest` (AVD + emuladores) | 3/3 |
| Functions unitarias | `cd firebase\functions; npm test` | 35/35 |
| Functions integración | `npm run test:integration` (cierra antes T1) | 13/13 |
| Security Rules | `npm run test:rules` (cierra antes T1) | 73/73 |
| Web unitarias | `cd web; npm test` | 110/110 |
| Web integración | `npm run test:integration` (cierra antes T1) | 15/15 |

> Las suites con `emulators:exec` levantan sus propios emuladores: cierra T1 antes y relanza
> después (y repite 0.4–0.6, porque borran los datos).

---

## 10. Qué NO se puede probar en este sprint

- Exportación/restauración real de HT-12, despliegue de Hosting y presupuesto (HT-02): requieren proyecto en la nube.
- Captura offline desde pantallas Android (jornadas, despachos, gastos): Sprint 3.
- Cierre forzado real con `adb shell am force-stop` sobre una captura: Sprint 3 (HT-07 lo cubre con `terminate()` en la prueba instrumentada).
- CI en verde y cobertura Android (Kover): Fase 0, pospuesta.
