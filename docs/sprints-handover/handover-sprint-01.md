# Informe de Relevo Técnico — Sprint 1: Cimientos Técnicos, Infraestructura Firebase y Acceso Seguro Móvil

- **Fecha de Cierre:** 03/11/2026
- **Desarrollador Responsable:** Bryan (`papasfritasdev`)
- **Asistente IA Utilizado:** Google Antigravity
- **Puntos de Historia Completados:** 23 / 23 Pts (100% de la capacidad del Sprint 1)

---

## 1. Historias Implementadas y Estado

| ID | Tipo | Título | Pts | Estado | Enlace / PR |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **HT-01** | Infraestructura | Repositorio, flujo de ramas y CI | 3 | Completada | PR #1 (`develop`) |
| **HT-02** | Infraestructura | Aprovisionamiento de Firebase y Emuladores | 5 | Completada | PR #1 (`develop`) |
| **HT-04** | Arquitectura | Autenticación y Custom Claims (`gestionarUsuario`) | 5 | Completada | PR #1 (`develop`) |
| **HT-03** | Arquitectura | Base arquitectónica Android (Clean Architecture + Compose) | 5 | Completada | PR #2 (`develop`) |
| **HT-06** | Spike Técnico | Persistencia Offline-First en Cloud Firestore | 3 | Completada | PR #2 (`develop`) |
| **HU-01a**| Historia Usuario | Inicio de sesión por rol en app móvil (RBAC) | 2 | Completada | PR #2 (`develop`) |

---

## 2. Mapa de Archivos Clave Creados o Modificados

### A. Subproyecto Móvil Android (`android/app/src/main/java/ec/silmay/app/`)
- **Capa Dominio (`domain/`) — Kotlin Puro (Cero dependencias de SDKs):**
  - Entidades:
    - [`Rol.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/domain/model/Rol.kt): Enums autoritativos (`operador`, `supervisor`, `contador`, `admin`).
    - [`UsuarioSesion.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/domain/model/UsuarioSesion.kt): Entidad inmutable de sesión en disco local.
    - [`AppMessage.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/domain/model/AppMessage.kt): Catálogo canónico oficial de mensajes `MSG-01` a `MSG-16`.
    - [`AppResult.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/domain/model/AppResult.kt): Encapsulador monádico funcional (`Success` y `Error`).
  - Repositorios (Interfaces):
    - [`AuthRepository.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/domain/repository/AuthRepository.kt): Contrato abstracto para autenticación y gestión de sesión.
  - Casos de Uso:
    - [`IniciarSesionUseCase.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/domain/usecase/IniciarSesionUseCase.kt): Reglas de negocio RBAC móvil (admite `operador` y `supervisor`; rechaza `admin` y `contador` con `MSG-13`; bloquea cuentas con `estado == false` con `MSG-03`).

- **Capa Datos (`data/`):**
  - Configuración y Proveedores:
    - [`FirestoreProvider.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/data/firestore/FirestoreProvider.kt): Persistent Disk Cache SQLite habilitada y utilitarios para alternar modo avión (`disableNetwork()`).
  - Mappers:
    - [`UsuarioMapper.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/data/mapper/UsuarioMapper.kt): Transformador bidireccional Firestore `DocumentSnapshot` $\rightarrow$ `UsuarioSesion`.
  - Repositorios (Implementaciones):
    - [`AuthRepositoryImpl.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/data/repository/AuthRepositoryImpl.kt): Integración con Firebase Auth, resolución de Custom Claims y lectura en caché local.
    - [`SpikeOfflineRepository.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/data/firestore/SpikeOfflineRepository.kt): Benchmark de persistencia offline y escritura atómica con UUID v4.

- **Capa Presentación (`presentation/`):**
  - Estado y Lógica:
    - [`LoginUiState.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/presentation/login/LoginUiState.kt): Estado inmutable reactivo (`StateFlow`).
    - [`LoginViewModel.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/presentation/login/LoginViewModel.kt): ViewModel Hilt con inyección de dependencias.
  - Vistas Jetpack Compose:
    - [`LoginScreen.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/presentation/login/LoginScreen.kt): Pantalla accesible con Compose Material 3, indicador de carga y despliegue estricto de mensajes normalizados.
    - [`MainActivity.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/MainActivity.kt): Punto de entrada anotado con `@AndroidEntryPoint`.

- **Inyección de Dependencias (`di/`):**
  - [`AppModule.kt`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/java/ec/silmay/app/di/AppModule.kt): Configuración de Singletons Hilt (`FirebaseAuth`, `FirebaseFirestore`, `IniciarSesionUseCase` y `bindAuthRepository`).

- **Recursos de UI (`res/`):**
  - [`strings.xml`](file:///D:/PROYECTO_SILMAY_APLI/android/app/src/main/res/values/strings.xml): Textos oficiales del catálogo `msg_01` a `msg_16`.

### B. Backend Serverless e Infraestructura (`firebase/`)
- **Cloud Functions (`firebase/functions/src/`):**
  - [`gestionarUsuario.ts`](file:///D:/PROYECTO_SILMAY_APLI/firebase/functions/src/gestionarUsuario.ts): Función callable autoritativa en `southamerica-east1` para alta y modificación de cuentas con asignación de Custom Claims.
  - [`validaciones.ts`](file:///D:/PROYECTO_SILMAY_APLI/firebase/functions/src/utils/validaciones.ts): Validador de cédula ecuatoriana de 10 dígitos (Algoritmo Módulo 10) y catálogo de roles.
  - [`index.ts`](file:///D:/PROYECTO_SILMAY_APLI/firebase/functions/src/index.ts): Exportación centralizada del microservicio.
- **Reglas e Índices:**
  - [`firestore.rules`](file:///D:/PROYECTO_SILMAY_APLI/firebase/firestore.rules): Reglas declarativas base autoritativas.
  - [`firestore.indexes.json`](file:///D:/PROYECTO_SILMAY_APLI/firebase/firestore.indexes.json): Definición canónica de índices.
  - [`.firebaserc`](file:///D:/PROYECTO_SILMAY_APLI/.firebaserc) y [`firebase.json`](file:///D:/PROYECTO_SILMAY_APLI/firebase.json): Configuración de proyectos (`silmay-dev`) y puertos de emuladores (Auth: 9099, Firestore: 8080, Functions: 5001, UI: 4000).

### C. Scripts de Bootstrapping y Pruebas (`scripts/`)
- [`crear-admin-inicial.ts`](file:///D:/PROYECTO_SILMAY_APLI/scripts/crear-admin-inicial.ts): Script idempotente para generar el usuario Administrador del sistema con `{ role: 'admin' }`.
- [`crear-operador-prueba.ts`](file:///D:/PROYECTO_SILMAY_APLI/scripts/crear-operador-prueba.ts): Script para generar el usuario Operador en emuladores locales con `{ role: 'operador' }`.

---

## 3. Contratos de Datos y Esquema Firestore

- **Colección `usuarios`:**
  - Registros de perfiles autoritativos según `docs/firebase-schema.md`.
  - Atributos clave: `uid` (PK), `nombres`, `cedula` (Módulo 10), `email`, `rol`, `zonaAsignada`, `estado`.
- **Cuentas de Prueba Creadas en el Emulador Local (`silmay-dev`):**
  1. **Administrador:**
     - Correo: `admin@silmay.ec`
     - Contraseña: `Silmay2026!Admin`
     - Custom Claim: `{ role: 'admin' }`
     - Documento: `usuarios/{uid}`
  2. **Operador:**
     - Correo: `operador@silmay.ec`
     - Contraseña: `Silmay2026!Operador`
     - Custom Claim: `{ role: 'operador' }`
     - Documento: `usuarios/OFW57GflFYTZyR9cnMZdVG6KriuI`

---

## 4. Auditoría de Calidad y Definición de Terminado (DoD)

- [x] **Criterios Gherkin Verificados (HU-01a):**
  - **CA1 (Acceso exitoso):** Validado para roles `operador` y `supervisor`.
  - **CA2 (Error de credenciales):** Validado con despliegue opaco de `MSG-01`.
  - **CA3 (Baja lógica):** Bloqueo y revocación inmediata ante `estado: false` con `MSG-03`.
  - **CA4 (Restricción RBAC móvil):** Cierre y bloqueo para `admin` y `contador` con `MSG-13`.
  - **CA5 (Primer login sin red):** Validado con `MSG-02`.
- [x] **Requisitos No Funcionales del Spike (HT-06):**
  - **RNF-01:** Confirmación de guardado atómico local certificada en $\le 2.0\text{ s}$ en disco físico.
  - **RNF-10:** Lectura persistida recuperada íntegramente desde `Source.CACHE` sin red.
  - **RNF-11:** Idempotencia absoluta con UUID v4 y `.set()`.
- [x] **Cobertura de Pruebas Unitarias:**
  - **Móvil Android:** **20 / 20 pruebas unitarias aprobadas al 100%** (`testDebugUnitTest`):
    - `IniciarSesionUseCaseTest`: 7 tests.
    - `SpikeOfflineRepositoryTest`: 6 tests.
    - `LoginViewModelTest`: 6 tests.
    - `ExampleUnitTest`: 1 test.
  - **Backend Serverless:** **15 / 15 pruebas unitarias aprobadas al 100%** (`vitest`):
    - `validaciones.test.ts`: 10 tests.
    - `gestionarUsuario.test.ts`: 5 tests.
- [x] **Clean Architecture Estricta:** Cero imports prohibidos (`android.*`, `compose.*`, `firebase.*`) dentro de `domain/`.
- [x] **Catálogo de Mensajes Normalizados:** Cero textos hardcodeados; todos los mensajes coinciden textualmente con `MSG-01` a `MSG-16`.
- [x] **Cero Secretos:** Repositorio hermético, `google-services.json` y credenciales excluidas de Git.
- [x] **Integración Continua (CI):** Todos los workflows de GitHub Actions finalizados en verde sobre `develop`.

---

## 5. Puntos de Integración y Advertencias para el Sprint 2

### Funciones e Interfaces Listas para Consumo en Sprint 2:
1. **Inyección de Dependencias Hilt:** `@AndroidEntryPoint` en `MainActivity` listo para alojar la navegación Compose hacia los módulos operativos.
2. **Cliente Firestore Offline-First:** `FirestoreProvider` ya instancia `FirebaseFirestore` con caché persistente y soporte para emulador local (`10.0.2.2:8080`).
3. **Mapeador de Sesión:** `UsuarioSesion` disponible como objeto inmutable tras el login para heredar la `zonaAsignada` y el `uid` del operador en las capturas de campo.

### Gotchas Técnicas y Advertencias:
1. **Regla de Oro de Idempotencia:** En el Sprint 2, al registrar jornadas de maquinaria pesada, está **terminantemente prohibido** utilizar `collection.add()`. Todo registro debe generar un `UUID.randomUUID().toString()` en el cliente y persistirse con `.document(uuid).set(...)`.
2. **Configuración de Emuladores en Modo Avión:** Para probar la persistencia offline de jornadas en el emulador de Android Studio, utilizar `FirestoreProvider.desactivarRed(firestore)` o activar el modo avión del emulador Android (`adb emu network speed edge` o desconexión de red).
3. **AGP 9.2 y KSP:** El proyecto móvil utiliza el soporte integrado de Kotlin de AGP 9.2 (`android.disallowKotlinSourceSets=false` en `gradle.properties`). Al agregar nuevas bibliotecas con procesadores de anotaciones, usar siempre `ksp(...)` y evitar KAPT.

### Objetivo de Arranque Sugerido para el Sprint 2:
- **Módulo M1 — Captura de Jornada y Maquinaria Pesada:**
  - **HT-07:** Configuración de colecciones operativas (`jornadas`, `maquinaria`, `catalogos/lugares`).
  - **HU-02:** Registro de inicio y fin de turno con validaciones matemáticas (horas brutas, horas efectivas, delta de horómetro $H_{fin} > H_{ini}$ y control de jornada abierta única `MSG-14`).
