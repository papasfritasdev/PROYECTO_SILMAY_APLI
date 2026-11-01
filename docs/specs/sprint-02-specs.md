# Especificación Funcional y Técnica — Sprint 2 (27 Puntos)

- **Documento Fuente:** Resumen Técnico Pila del Producto SILMAY v2.0
- **Objetivo del Sprint:** Modelo de datos, Security Rules, base web y datos maestros (usuarios y maquinaria).
- **Capacidad del Sprint:** 27 Puntos de Historia (de un total del backlog general de 128 pts: 76 HU + 52 HT).
- **Entregables:** HT-05, HT-07, HT-08, HT-12, HU-01b, HU-02, HU-09.

---

## 1. Inventario de Historias del Sprint 2

| ID | Tipo | Título | Pts | Responsabilidad Principal |
| :--- | :--- | :--- | :--- | :--- |
| **HT-05** | Arquitectura | Modelo de datos y Security Rules | 5 | Colecciones canónicas, reglas RBAC autoritativas, test en emulador y script de catálogos. |
| **HT-07** | Arquitectura | Capa de persistencia local y sync | 8 | Repositorios Android con `set()` y UUID v4, cola FIFO (50 mutaciones) y auditoría de rechazos. |
| **HT-08** | Arquitectura | Base de la plataforma web React | 3 | Configuración React 18 + Vite + TS, rutas protegidas por rol y desconexión a los 30 min. |
| **HT-12** | Infraestructura | Respaldo mensual de Firestore | 2 | Cloud Function cron mensual hacia Google Cloud Storage y validación de restauración. |
| **HU-01b**| Historia Usuario | Inicio de sesión por rol en web | 1 | Login web para `contador`/`admin`, bloqueo de `operador`/`supervisor` con `MSG-13` e inactividad. |
| **HU-02** | Historia Usuario | Gestión de usuarios y roles | 5 | CRUD administrativo con Custom Claims, validación de cédula (10 dígitos), baja lógica y auditoría. |
| **HU-09** | Historia Usuario | Gestión de maquinaria y volquetas | 3 | Registro de flota y volquetas unificadas, bloqueo de duplicados con `MSG-09` y protección por rol. |

---

## 2. Detalle de Historias Técnicas

### HT-05: Modelo de datos y Security Rules (5 pts) [RF-18, RNF-06]
- **Alcance:**
  1. Definición de las 12 colecciones canónicas en Firestore e índices compuestos en `firestore.indexes.json`.
  2. Implementación de `firestore.rules` evaluando autenticación, Custom Claim `role`, titularidad (`isOwner`), visibilidad por `zona` para supervisores e inmutabilidad de jornadas cerradas (`estado == 'abierta'` para updates desde móvil).
  3. Prohibición estricta de borrado físico (`allow delete: if false;`) en colecciones operativas y maestras.
  4. Batería de pruebas automatizadas 100% en el emulador local verificando accesos permitidos y denegados.
  5. Script `scripts/sembrar-catalogos.ts` para carga inicial de catálogos maestros (`materiales`, `clientes`, `lugares`, `rubros`, `motivos`) y `parametros`.

### HT-07: Capa de persistencia local y sync (8 pts) [RF-16, 17, 19]
- **Alcance:**
  1. Implementación en la capa `data` de Android de los repositorios locales utilizando obligatoriamente `db.collection(...).document(uuidV4).set(...)`.
  2. Inyección automática de los 6 metadatos obligatorios de control en cada registro operativo: `uuid`, `createdAtClient`, `syncedAtServer`, `userId`, `deviceId` y `zona`.
  3. Garantizar la preservación de al menos 50 mutaciones encoladas en disco ante cierre forzado de la app o reinicio del dispositivo, procesadas en orden FIFO al recuperar red.
  4. Captura y registro en la colección `auditoria` ante rechazos del servidor por `PERMISSION_DENIED`.

### HT-08: Base de la plataforma web React (3 pts) [RF-02, RNF-07]
- **Alcance:**
  1. Inicialización del proyecto en `web/` con React 18, Vite y TypeScript bajo arquitectura limpia (`domain/`, `data/`, `hooks/`, `features/`).
  2. Implementación de enrutamiento protegido (Guards) basado en el Custom Claim `role` del token JWT.
  3. Temporizador global de actividad (hook de sesión) que ejecuta el cierre automático de la sesión web tras 30 minutos continuos sin interacción del usuario (RNF-07).
  4. Configuración de despliegue para entornos `silmay-dev` y `silmay-prod` en Firebase Hosting.

### HT-12: Respaldo mensual de Firestore (2 pts) [RNF-16]
- **Alcance:**
  1. Implementación de la Cloud Function programada `respaldoMensual.ts` activada mediante cron mensual (Cloud Pub/Sub).
  2. Exportación completa de las colecciones de Firestore hacia un bucket seguro en Google Cloud Storage con procedimiento de restauración documentado y validado.

---

## 3. Especificación de Historias de Usuario (Formato Gherkin)

### HU-01b: Inicio de sesión por rol en plataforma web (1 pt) — Rol: Contador / Admin

#### CA1: Acceso autorizado según perfil administrativo
- **Dado** que un usuario tiene una cuenta activa (`estado: true`) con rol `contador` o `admin`,
- **Cuando** ingresa sus credenciales válidas en la plataforma web React,
- **Entonces** el sistema autentica la sesión y despliega el panel principal mostrando únicamente las opciones autorizadas para su rol en la matriz RBAC.

#### CA2: Bloqueo de roles de campo en plataforma web
- **Dado** que un usuario con rol `operador` o `supervisor` intenta iniciar sesión en la plataforma web,
- **Cuando** envía el formulario de acceso,
- **Entonces** el sistema bloquea el ingreso, cierra la sesión inmediatamente y muestra exactamente el mensaje:
  > `"Esta plataforma no está habilitada para su rol."` (`MSG-13`)

#### CA3: Desconexión automática por inactividad
- **Dado** que un Contador o Administrador mantiene una sesión abierta en el navegador web,
- **Cuando** transcurren 30 minutos continuos sin eventos de interacción (teclado o ratón),
- **Entonces** el sistema cierra la sesión automáticamente y redirige a la pantalla de login (RNF-07).

---

### HU-02: Gestión de usuarios y roles (5 pts) — Rol: Administrador

#### CA1: Creación de cuenta e inyección de Custom Claim
- **Dado** que el Administrador completa el formulario de alta de usuario con nombres, cédula ecuatoriana (10 dígitos), correo, teléfono, zona asignada, cargo y rol,
- **Cuando** confirma el registro,
- **Entonces** se invoca la Cloud Function `gestionarUsuario`, se crea la cuenta en Firebase Auth con contraseña mínima de 8 caracteres alfanuméricos, se inyecta el atributo `role` como Custom Claim y se guarda el perfil en la colección `usuarios` con `estado: true`.

#### CA2: Validación de unicidad de correo y cédula
- **Dado** que el Administrador intenta registrar o actualizar un usuario,
- **Cuando** ingresa un correo electrónico o un número de cédula de 10 dígitos que ya existe en la colección `usuarios`,
- **Entonces** el sistema bloquea el guardado y notifica la duplicidad en pantalla.

#### CA3: Desactivación mediante baja lógica
- **Dado** que el Administrador requiere dar de baja a un usuario existente,
- **Cuando** cambia su estado a inactivo (`estado: false`),
- **Entonces** el sistema revoca su acceso futuro en móvil y web (emitiendo `MSG-03` si intenta entrar), pero preserva intactos todos sus registros históricos en `jornadas`, `despachos` y `gastos`.

#### CA4: Trazabilidad en auditoría
- **Dado** que el Administrador crea, modifica o desactiva una cuenta de usuario,
- **Cuando** la transacción se completa en el backend,
- **Entonces** se escribe un registro inmutable en la colección `auditoria` con el UID del autor, la acción ejecutada y `serverTimestamp`.

---

### HU-09: Gestión de maquinaria y volquetas (3 pts) — Rol: Administrador

#### CA1: Registro unificado de maquinaria y volquetas
- **Dado** que el Administrador accede al catálogo de flota en la web,
- **Cuando** registra una unidad ingresando `codigoInterno`, `placa`, `marca`, `modelo`, `tipoEquipo` (`excavadora`, `cargadora`, `retroexcavadora`, `tractocamión` o `volqueta`), `horometroBase` e `intervaloMantenimiento`,
- **Entonces** el sistema calcula `proximoMantenimientoHoras = horometroBase + intervaloMantenimiento` y persiste el documento en la colección `maquinaria` con `estado: 'operativa'`.

#### CA2: Prevención de duplicados de código o placa
- **Dado** que el Administrador intenta guardar una unidad de maquinaria o volqueta,
- **Cuando** el `codigoInterno` o la `placa` ya se encuentran registrados en la colección `maquinaria`,
- **Entonces** el sistema impide el guardado y muestra exactamente el mensaje:
  > `"Ya existe una unidad de maquinaria registrada con ese código/placa."` (`MSG-09`)

#### CA3: Protección autoritativa de escritura en servidor
- **Dado** que un usuario con rol distinto de `admin` (`operador`, `supervisor` o `contador`) intenta crear o modificar un documento en `maquinaria`,
- **Cuando** la petición llega a Cloud Firestore,
- **Entonces** las Security Rules rechazan la operación con error `PERMISSION_DENIED` y se registra el evento en `auditoria`.