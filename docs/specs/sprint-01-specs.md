# Especificación Funcional y Técnica — Sprint 1 (23 Puntos)

- **Documento Fuente:** Resumen Técnico Pila del Producto SILMAY v2.0
- **Objetivo del Sprint:** Cimientos técnicos, infraestructura Firebase y acceso seguro por rol en la app móvil.
- **Capacidad del Sprint:** 23 Puntos de Historia (de un total del backlog general de 128 pts: 76 HU + 52 HT).
- **Entregables:** HT-01, HT-02, HT-03, HT-04, HT-06, HU-01a.

---

## 1. Inventario de Historias del Sprint 1

| ID | Tipo | Título | Pts | Responsabilidad Principal |
| :--- | :--- | :--- | :--- | :--- |
| **HT-01** | Infraestructura | Repositorio, flujo de ramas y CI | 3 | Monorepo Git, reglas de PR y pipeline de GitHub Actions (cobertura ≥ 70%). |
| **HT-02** | Infraestructura | Aprovisionamiento de Firebase | 5 | Proyectos `silmay-dev` y `silmay-prod`, emuladores y alertas de presupuesto. |
| **HT-03** | Arquitectura | Base arquitectónica Android | 5 | Clean Architecture + Hilt en Kotlin, Compose, `minSdk 26`, `allowBackup="false"`. |
| **HT-04** | Arquitectura | Autenticación y Custom Claims | 5 | Cloud Function `gestionarUsuario`, validación de rol y script de admin inicial. |
| **HT-06** | Spike Técnico | Persistencia Offline-First | 3 | Spike de caché en disco de Firestore vs Room ante cortes y reinicios. |
| **HU-01a**| Historia Usuario | Inicio de sesión por rol en móvil | 2 | Pantalla Compose, RBAC móvil (`operador`/`supervisor`), mensajes `MSG-01`, `02`, `03`, `13`. |

---

## 2. Detalle de Historias Técnicas

### HT-01: Repositorio, flujo de ramas y CI (3 pts) [RNF-15]
- **Alcance:**
  1. Estructura física monorepo (`android/`, `web/`, `firebase/`, `scripts/`, `docs/`).
  2. Ramas protegidas `main` y `develop`.
  3. Workflow de GitHub Actions que ejecute:
     - Verificación de no-importaciones prohibidas en `ec.silmay.app.domain`.
     - Pruebas unitarias en Android con reporte de cobertura Kover (≥ 70% en Dominio).

### HT-02: Aprovisionamiento de Firebase (5 pts) [RNF-13]
- **Alcance:**
  1. Creación de `silmay-dev` y `silmay-prod` en plan Blaze.
  2. Firestore y Functions configurados en `southamerica-east1` (ADR-0009).
  3. Emuladores locales configurados y operativos (`auth`, `firestore`, `functions`).
  4. Presupuesto en Google Cloud Billing con alertas escalonadas al 50%, 90% y 100% de USD 20,00/mes.

### HT-03: Base arquitectónica Android (5 pts) [RNF-14, 17, 19]
- **Alcance:**
  1. Proyecto Android con Kotlin DSL, Jetpack Compose y Material 3.
  2. Configuración de `minSdk 26` y directiva obligatoria `android:allowBackup="false"` en `AndroidManifest.xml` (RNF-05).
  3. Estructura de paquetes: `domain/`, `data/`, `presentation/`, `di/`.
  4. Configuración de Hilt para inyección de dependencias.

### HT-04: Autenticación y Custom Claims (5 pts) [RF-02, 03, 05]
- **Alcance:**
  1. Cloud Function callable `gestionarUsuario.ts` para asignar claim `role` (`operador`, `supervisor`, `contador`, `admin`).
  2. Validación de formato: cédula ecuatoriana de 10 dígitos y correo únicos.
  3. Script local `scripts/crear-admin-inicial.ts` para crear el primer admin con Firebase Admin SDK.

### HT-06: Spike de Persistencia Offline-First (3 pts) [RNF-01, 10, 11]
- **Alcance:**
  1. Prueba experimental con Firestore Persistent Disk Cache habilitada en Android.
  2. Verificación de confirmación de guardado local en ≤ 2.0 s en modo avión (RNF-01).
  3. Validación de tolerancia ante cierre forzado del proceso en memoria.

---

## 3. Especificación de HU-01a: Inicio de Sesión por Rol en App Móvil (2 pts)

### Criterios de Aceptación (Formato Gherkin)

#### CA1: Acceso exitoso con credenciales y rol válido
- **Dado** que el usuario tiene una cuenta activa (`estado: true`) con rol `operador` o `supervisor`,
- **Cuando** ingresa su correo electrónico y contraseña válidos en la app móvil y presiona "Iniciar Sesión",
- **Entonces** el sistema valida el JWT token y su Custom Claim, guarda la sesión en disco local y navega al menú principal correspondiente a su rol operativo.

#### CA2: Error ante credenciales incorrectas (Mensaje Opaco)
- **Dado** que el usuario ingresa un correo o una contraseña que no coinciden en Firebase Auth,
- **Cuando** presiona "Iniciar Sesión",
- **Entonces** el sistema no autentica la sesión, permanece en la pantalla de login y muestra exactamente el mensaje:
  > `"Credenciales no válidas. Revise su usuario o contraseña."` (`MSG-01`)
- **Y** no revela si el error proviene del correo o de la contraseña.

#### CA3: Bloqueo de cuenta deshabilitada (Baja Lógica)
- **Dado** que un usuario posee credenciales correctas pero su registro en `usuarios` tiene `estado: false`,
- **Cuando** intenta iniciar sesión en la app móvil,
- **Entonces** el sistema revoca la sesión y muestra exactamente el mensaje:
  > `"Cuenta deshabilitada. Contacte con la administración de SILMAY."` (`MSG-03`)

#### CA4: Restricción de plataforma por rol (RBAC Móvil)
- **Dado** que un usuario con rol `contador` o `admin` intenta autenticarse en la app móvil,
- **Cuando** presiona "Iniciar Sesión",
- **Entonces** el sistema cierra la sesión inmediatamente, no permite la entrada y despliega en pantalla:
  > `"Esta plataforma no está habilitada para su rol."` (`MSG-13`)

#### CA5: Primer inicio de sesión sin conectividad
- **Dado** que la app se ejecuta por primera vez en un dispositivo sin conexión a Internet,
- **Cuando** el usuario intenta iniciar sesión,
- **Entonces** el sistema bloquea el intento y muestra:
  > `"Se requiere conexión a Internet para el registro inicial de su cuenta en este dispositivo."` (`MSG-02`)