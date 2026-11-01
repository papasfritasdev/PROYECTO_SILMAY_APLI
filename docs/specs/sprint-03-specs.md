# Especificación Funcional y Técnica — Sprint 3 (26 Puntos)

- **Documento Fuente:** Resumen Técnico Pila del Producto SILMAY v2.0
- **Objetivo del Sprint:** Captura en campo Offline-First: jornadas, despachos y sincronización diferida.
- **Capacidad del Sprint:** 26 Puntos de Historia (de un total del backlog general de 128 pts: 76 HU + 52 HT).
- **Entregables:** HU-07, HU-03, HU-04, HU-08.

---

## 1. Inventario de Historias del Sprint 3

| ID | Tipo | Título | Pts | Responsabilidad Principal |
| :--- | :--- | :--- | :--- | :--- |
| **HU-07** | Historia Usuario | Uso de la aplicación sin conexión | 5 | Operación plena en modo avión, caché de catálogos y protección de cierre de sesión con pendientes. |
| **HU-03** | Historia Usuario | Registro de venta y despacho de pétreos | 8 | Formulario offline con confirmación ≤ 2 s, alertas hápticas, control de memoria (`MSG-04`) y UUID v4. |
| **HU-04** | Historia Usuario | Registro de jornada de trabajo | 5 | Apertura/cierre de turno, cálculo de Horas Efectivas y Δ Horómetro, validaciones `MSG-05`, `11`, `12`, `14`. |
| **HU-08** | Historia Usuario | Sincronización automática de registros | 8 | Cola FIFO en segundo plano con backoff exponencial, idempotencia sin duplicados y métrica TES. |

---

## 2. Reglas de Negocio y Fórmulas Aplicables en este Sprint

1. **Fórmulas de Cierre de Jornada (RF-09):**
   - $\text{Horas Brutas} = \text{Hora\_Salida} - \text{Hora\_Inicio}$
   - $\text{Horas Efectivas} = \text{Horas\_Brutas} - \sum \text{Horas\_Restadas}$
   - $\Delta \text{ Horómetro} = \text{Horómetro\_Final} - \text{Horómetro\_Inicial}$
2. **Idempotencia y Rendimiento (RNF-01, RNF-11):**
   - Toda escritura se realiza en la caché local de Firestore con `set()` y `uuid` (UUID v4 generado en cliente).
   - Confirmación visual en pantalla en $\le 2.0\text{ segundos}$ sin importar la cobertura celular.

---

## 3. Especificación de Historias de Usuario (Formato Gherkin)

### HU-07: Uso de la aplicación sin conexión (5 pts) — Rol: Operador / Supervisor

#### CA1: Exigencia de red en el primer inicio de sesión
- **Dado** que el usuario instala o abre la aplicación por primera vez en el dispositivo sin tener sesión previa cacheada,
- **Cuando** intenta iniciar sesión estando sin conexión a Internet,
- **Entonces** el sistema bloquea el acceso y muestra exactamente el mensaje:
  > `"Se requiere conexión a Internet para el registro inicial de su cuenta en este dispositivo."` (`MSG-02`)

#### CA2: Operación autónoma en modo avión tras primer login
- **Dado** que el usuario ya realizó su primer inicio de sesión con red y descargó su token y los catálogos maestros (`materiales`, `clientes`, `lugares`, `maquinaria`),
- **Cuando** ingresa a los frentes de extracción sin cobertura o en modo avión,
- **Entonces** todas las pantallas de captura operativa cargan los catálogos desde el disco local y permiten registrar transacciones con 100% de disponibilidad (RNF-12).

#### CA3: Protección de datos ante intento de cierre de sesión
- **Dado** que existen registros locales en estado `'Pendiente de sincronización'` en la cola del dispositivo,
- **Cuando** el usuario presiona la opción "Cerrar Sesión",
- **Entonces** el sistema advierte al usuario que existen registros pendientes de subir, bloquea el cierre de sesión y preserva intactos los datos en disco (RF-04).

#### CA4: Actualización reactiva de catálogos al recuperar red
- **Dado** que el Administrador agregó o desactivó ítems en `catalogos/{tipo}/items` o `maquinaria` desde la web,
- **Cuando** el dispositivo móvil recupera conectividad celular o Wi-Fi,
- **Entonces** la caché local del SDK actualiza automáticamente las listas desplegables disponibles para el operador.

---

### HU-03: Registro de venta y despacho de pétreos (8 pts) — Rol: Operador / Supervisor

#### CA1: Guardado local inmediato en modo desconectado
- **Dado** que el operador completa el formulario de despacho seleccionando material (`arena normal`, `arena fina`, `piedra`, `ripio`), unidad (`m3` o `viajes`), cantidad ($> 0$), cliente, volqueta asignada, lugar (heredado de la jornada activa), método de cobro (`efectivo`, `transferencia`, `credito`) y `valorUSD` ($> 0$),
- **Cuando** presiona "Guardar" estando sin conexión,
- **Entonces** el registro se almacena en disco en $\le 2.0\text{ segundos}$ (RNF-01) con `estadoValidacion: 'pendiente'` y estado visual `'Pendiente de sincronización'`.

#### CA2: Validación local inmediata con aviso háptico
- **Dado** que el operador intenta guardar un despacho dejando campos obligatorios vacíos o ingresando `cantidad <= 0` o `valorUSD <= 0`,
- **Cuando** presiona "Guardar",
- **Entonces** el sistema bloquea la persistencia, resalta los campos erróneos en pantalla y emite retroalimentación háptica/sonora inmediata (RF-12).

#### CA3: Control de almacenamiento lleno en el dispositivo
- **Dado** que la memoria interna del dispositivo móvil se encuentra llena y el guardado en disco lanza una excepción de espacio,
- **Cuando** el operador intenta guardar el despacho,
- **Entonces** el sistema no descarta los datos digitados en el formulario y despliega en pantalla:
  > `"Memoria insuficiente en el dispositivo para guardar el registro."` (`MSG-04`)

#### CA4: Generación de UUID v4 y metadatos de control
- **Dado** que se guarda un nuevo despacho en el cliente móvil,
- **Cuando** se escribe el documento en la colección `despachos`,
- **Entonces** se utiliza como Document ID un `uuid` (UUID v4) generado localmente e incluye obligatoriamente `createdAtClient`, `userId`, `deviceId` y `zona`.

---

### HU-04: Registro de jornada de trabajo (5 pts) — Rol: Operador / Supervisor

#### CA1: Apertura de jornada con sugerencia de horómetro
- **Dado** que el operador no tiene ninguna jornada activa en curso,
- **Cuando** inicia una nueva jornada seleccionando `maquinaId`, `lugarId`, `horaInicio` (propuesta por el reloj del dispositivo, editable) y `horometroInicial` (el sistema sugiere el último horómetro final registrado para esa máquina),
- **Entonces** se crea el documento local en `jornadas` con `estado: 'abierta'`, `procesada: false` y `tieneFallaCritica: false`.

#### CA2: Cierre de jornada y cálculo automático de métricas
- **Dado** que el operador tiene una jornada en `estado: 'abierta'`,
- **Cuando** registra `horaSalida`, `horometroFinal` y `observaciones` válidas y confirma el cierre,
- **Entonces** el sistema calcula automáticamente `horasBrutas`, `horasEfectivas` ($\text{Horas Brutas} - \sum \text{Horas Restadas}$) y `deltaHorometro` ($\text{Horómetro Final} - \text{Horómetro Inicial}$), cambia el estado a `'cerrada'` y bloquea cualquier edición posterior desde el móvil (RF-09, RF-10).

#### CA3: Bloqueo por inconsistencias en horómetro, horas o deducciones
- **Dado** que el operador intenta cerrar su jornada abierta,
- **Cuando** ingresa un `horometroFinal <= horometroInicial`,
- **Entonces** se bloquea el cierre y se muestra:
  > `"El horómetro final no puede ser menor al horómetro inicial. Por favor verifique el tablero de la máquina."` (`MSG-05`)
- **Y cuando** ingresa una `horaSalida <= horaInicio`, se bloquea el cierre y se muestra:
  > `"La hora de salida debe ser posterior a la hora de inicio de la jornada."` (`MSG-11`)
- **Y cuando** la sumatoria de `horasRestadas >= horasBrutas`, se bloquea el cierre y se muestra:
  > `"Las horas restadas no pueden superar la duración de la jornada."` (`MSG-12`)

#### CA4: Restricción de jornada única abierta por operador
- **Dado** que el operador ya posee una jornada con `estado: 'abierta'`,
- **Cuando** intenta iniciar una segunda jornada simultánea,
- **Entonces** el sistema impide la acción y muestra exactamente el mensaje:
  > `"Tiene una jornada abierta. Finalícela antes de iniciar otra."` (`MSG-14`)

---

### HU-08: Sincronización automática de registros (8 pts) — Rol: Operador / Supervisor

#### CA1: Envío automático en orden FIFO al recuperar red
- **Dado** que el dispositivo móvil acumula registros locales en estado `'Pendiente de sincronización'` en `jornadas`, `despachos` o `gastos`,
- **Cuando** el monitor de conectividad detecta el restablecimiento de red (3G/4G/Wi-Fi),
- **Entonces** las mutaciones pendientes se transmiten automáticamente en segundo plano en estricto orden cronológico (FIFO) y su indicador visual en el móvil cambia a `'Sincronizado'` (RF-16).

#### CA2: Reintentos idempotentes ante caídas de red
- **Dado** que la conexión se interrumpe a mitad del envío de un lote de registros o el servidor no responde,
- **Cuando** el SDK reintenta la transmisión con espera exponencial (*exponential backoff*),
- **Entonces** la escritura mediante `set()` con el mismo `uuid` (UUID v4) sobrescribe idénticamente el documento en Firestore sin generar duplicados (RF-17, RNF-11).

#### CA3: Manejo y auditoría de rechazos por Security Rules
- **Dado** que un documento transmitido desde el móvil incumple una regla de integridad o permisos en `firestore.rules`,
- **Cuando** el servidor devuelve un rechazo definitivo,
- **Entonces** el registro local se marca con estado `'Rechazado'` y se escribe una entrada en la colección `auditoria` detallando el fallo (RF-18).

#### CA4: Trazabilidad para la métrica TES
- **Dado** que un documento operativo culmina su sincronización en Cloud Firestore,
- **Cuando** se persiste en el servidor,
- **Entonces** conserva intactos los atributos `createdAtClient`, `syncedAtServer` (`serverTimestamp`) y `deviceId` para el cálculo de la Tasa de Entregabilidad de Sincronización ($TES \ge 99.0\%$) (RF-19, RNF-09).