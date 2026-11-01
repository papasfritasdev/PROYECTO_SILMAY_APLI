# Especificación Funcional y Técnica — Sprint 4 (25 Puntos)

- **Documento Fuente:** Resumen Técnico Pila del Producto SILMAY v2.0
- **Objetivo del Sprint:** Horas restadas/novedades, gastos diarios, procesamiento en nube y mantenimiento.
- **Capacidad del Sprint:** 25 Puntos de Historia (de un total del backlog general de 128 pts: 76 HU + 52 HT).
- **Entregables:** HT-09, HU-05, HU-06, HU-10, HU-11.

---

## 1. Inventario de Historias del Sprint 4

| ID | Tipo | Título | Pts | Responsabilidad Principal |
| :--- | :--- | :--- | :--- | :--- |
| **HT-09** | Arquitectura | Cloud Function: jornadas y alertas | 5 | Trigger `procesarJornada` (`onDocumentWritten`), suma ACID de horómetro y motor de alertas. |
| **HU-05** | Historia Usuario | Horas restadas y novedades de jornada | 5 | Deducciones embebidas en jornada, fallas con severidad, `MSG-06` y captura puntual GNSS. |
| **HU-06** | Historia Usuario | Registro de gastos diarios | 5 | Captura offline de egresos, validación `MSG-07`, comprobante y totalizador diario en pantalla. |
| **HU-10** | Historia Usuario | Seguimiento mantenimiento preventivo | 5 | Semáforo de flota en web (verde/amarillo/rojo) y registro de mantenimientos ejecutados. |
| **HU-11** | Historia Usuario | Alertas de falla crítica y mantenimiento | 5 | Bandeja reactiva en tiempo real, idempotencia de alertas y atención exclusiva por Admin. |

---

## 2. Detalle de Historia Técnica

### HT-09: Cloud Function: jornadas y alertas (5 pts) [RF-20, RF-21, RNF-11]
- **Alcance:**
  1. Implementación del trigger `procesarJornada.ts` suscrito a `onDocumentWritten('jornadas/{id}')` en `southamerica-east1`.
  2. Evaluación de guardia de idempotencia: solo ejecuta si `after.estado === 'cerrada'` y `after.procesada === false`.
  3. En una transacción atómica ACID de Firestore:
     - Verifica `horasEfectivas` y `deltaHorometro`.
     - Incrementa `horometroActual` en el documento respectivo de `maquinaria` sumando el `deltaHorometro`.
     - Marca `procesada: true` en la jornada.
     - Si `tieneFallaCritica === true` y `notificadaFalla === false`, crea la alerta en `notificaciones` con ID determinista `falla_{jornadaId}`, `severidad: 'critica'` y marca `notificadaFalla: true`.
     - Si el nuevo `horometroActual >= proximoMantenimientoHoras`, actualiza `maquinaria.estado = 'requiere_mantenimiento'` y emite notificación preventiva con ID determinista `maint_{maquinaId}_{horometro}`.

---

## 3. Especificación de Historias de Usuario (Formato Gherkin)

### HU-05: Horas restadas y novedades de la jornada (5 pts) — Rol: Operador / Supervisor

#### CA1: Adición de deducciones embebidas en la jornada abierta
- **Dado** que el operador tiene una jornada en `estado: 'abierta'`,
- **Cuando** agrega una deducción seleccionando el motivo del catálogo (`almuerzo`, `traslado`, `clima o vía bloqueada`, `falla mecánica`, `hidráulica`, `neumáticos`, `otro`) e ingresa una duración en horas con 1 decimal ($> 0$),
- **Entonces** el registro se añade al arreglo embebido `deducciones` dentro del documento de la jornada y actualiza el acumulador `horasRestadas` (ADR-0007, RF-07).

#### CA2: Validación de descripción mínima en novedades de falla
- **Dado** que el motivo de la deducción corresponde a una falla técnica (`mecánica`, `hidráulica` o `neumáticos`),
- **Cuando** el operador intenta guardar la novedad con una descripción menor a 10 caracteres,
- **Entonces** el sistema rechaza el guardado y muestra exactamente el mensaje:
  > `"La descripción de la novedad debe tener al menos 10 caracteres."` (`MSG-06`)

#### CA3: Activación de bandera por falla de severidad crítica
- **Dado** que el operador registra una novedad de falla con descripción válida ($\ge 10\text{ caracteres}$) y selecciona `severidad: 'critica'`,
- **Cuando** guarda la deducción en la jornada,
- **Entonces** se activa automáticamente la bandera `tieneFallaCritica: true` en el documento raíz de la jornada (ADR-0008, RF-08).

#### CA4: Captura puntual y opcional de coordenadas GNSS
- **Dado** que el operador reporta una novedad de falla técnica,
- **Cuando** ha otorgado permiso explícito de ubicación al dispositivo, se adjuntan las coordenadas puntuales `{ lat, lng }` a la deducción;
- **Pero si** el operador deniega el permiso o no hay señal GPS disponible, la novedad se guarda normalmente con `coordenadas: null` sin bloquear la operación (RNF-21).

---

### HU-06: Registro de gastos diarios (5 pts) — Rol: Operador / Supervisor

#### CA1: Captura offline de egresos de campo
- **Dado** que el operador o supervisor accede al módulo de gastos en la app móvil,
- **Cuando** selecciona el `rubro` (`combustible`, `viáticos`, `repuestos`, `otros`), ingresa el `montoUSD` (2 decimales), el `numeroComprobante` físico y la unidad receptora (`unidadId`: máquina o volqueta),
- **Entonces** el sistema guarda el documento en la caché local de la colección `gastos` en $\le 2.0\text{ segundos}$ con `uuid` (UUID v4) y `estadoValidacion: 'pendiente'` (RF-14).

#### CA2: Validación de monto monetario válido
- **Dado** que el usuario intenta guardar un gasto diario,
- **Cuando** ingresa un monto menor o igual a cero (`montoUSD <= 0`) o con formato numérico inválido,
- **Entonces** el sistema bloquea el registro y despliega exactamente el mensaje:
  > `"Ingrese un valor monetario válido."` (`MSG-07`)

#### CA3: Totalizador diario en tiempo real en la interfaz móvil
- **Dado** que el usuario registra uno o varios gastos durante la fecha en curso,
- **Cuando** visualiza la pantalla de gastos diarios en el móvil,
- **Entonces** la interfaz calcula y muestra en tiempo real la sumatoria acumulada ($\sum \text{montoUSD}$) de los gastos registrados por ese usuario en el día para su control de caja chica (RF-15).

#### CA4: Descripción obligatoria para el rubro 'Otros'
- **Dado** que el usuario selecciona el rubro `'otros'` en el formulario de gasto,
- **Cuando** intenta guardar dejando el campo `descripcion` vacío,
- **Entonces** el sistema impide el guardado y exige detallar la naturaleza del egreso (RF-14).

---

### HU-10: Seguimiento del mantenimiento preventivo (5 pts) — Rol: Administrador

#### CA1: Indicador preventivo amarillo al alcanzar el umbral configurable
- **Dado** que una máquina tiene un `intervaloMantenimiento` y el parámetro global `umbralProximoMantenimiento` está en $0.90$ ($90\%$),
- **Cuando** su `horometroActual` se ubica en el rango $(\text{intervalo} \times \text{umbral}) \le \text{horometroActual} < \text{intervalo}$,
- **Entonces** la unidad se visualiza en color **Amarillo (Próximo)** en el semáforo de flota de la plataforma web (RF-29).

#### CA2: Indicador rojo al superar el límite de mantenimiento
- **Dado** que el horómetro acumulado de una máquina alcanza o supera su límite,
- **Cuando** $\text{horometroActual} \ge \text{proximoMantenimientoHoras}$ (o $\ge \text{intervalo}$),
- **Entonces** la unidad cambia a estado `'requiere_mantenimiento'` y se muestra en color **Rojo (Mantenimiento Vencido)** en el panel web (RF-29).

#### CA3: Registro de servicio ejecutado y restablecimiento a verde
- **Dado** que el Administrador registra un mantenimiento realizado en la colección `mantenimientos` ingresando `fecha`, `costoUSD`, arreglo de `repuestos` y `horometroServicio`,
- **Cuando** confirma la operación,
- **Entonces** el sistema recalcula automáticamente `proximoMantenimientoHoras = horometroServicio + intervaloMantenimiento`, actualiza la máquina a `estado: 'operativa'` y el semáforo vuelve a color **Verde (Óptimo)** (RF-31).

---

### HU-11: Alertas de falla crítica y mantenimiento (5 pts) — Rol: Administrador / Contador

#### CA1: Recepción reactiva en pantalla sin recargar la página
- **Dado** que el Administrador o el Contador tienen abierta la plataforma web,
- **Cuando** `procesarJornada` inserta un nuevo documento en la colección `notificaciones` por falla crítica o límite de mantenimiento,
- **Entonces** la bandeja de alertas en React se actualiza en tiempo real mediante suscripción WebSockets (`onSnapshot`) sin necesidad de recargar el navegador (RF-23).

#### CA2: Idempotencia ante reejecución de eventos en la nube
- **Dado** que una jornada cerrada con falla crítica ya fue procesada por la Cloud Function,
- **Cuando** ocurre una reentrega del evento en el backend,
- **Entonces** la validación de las banderas `procesada` y `notificadaFalla` junto al ID determinista impiden que se duplique la alerta en `notificaciones` (RNF-11).

#### CA3: Atención formal de alertas por el Administrador
- **Dado** que existe una alerta pendiente (`atendida: false`) en la bandeja web,
- **Cuando** el Administrador acciona el botón "Marcar como atendida",
- **Entonces** el documento se actualiza con `atendida: true`, registra el UID del Administrador en `atendidaPor` y la fecha de atención (RF-23).

#### CA4: Acceso de solo lectura para el perfil Contador
- **Dado** que un usuario autenticado con rol `contador` visualiza la bandeja de alertas,
- **Cuando** consulta las notificaciones activas,
- **Entonces** la interfaz oculta o deshabilita el botón de atención y las Security Rules bloquean cualquier intento de escritura sobre `notificaciones` para su rol (RF-23).