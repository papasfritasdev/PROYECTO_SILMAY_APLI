# Protocolo de Persistencia y Sincronización Diferida (Offline-First) — SILMAY

Este documento especifica el pipeline técnico de seis etapas para la captura de datos en mina, almacenamiento local seguro y transmisión asíncrona hacia Firebase[cite: 4, 6].

---

## 1. Principio Arquitectónico Fundamental

> **Condición Normal:** En las canteras y frentes de trabajo de SILMAY, la ausencia de cobertura celular o Wi-Fi es la condición ordinaria de operación[cite: 4, 6]. 
> La aplicación Android debe ser 100% operativa en modo avión tras el primer inicio de sesión[cite: 5, 6]. La red actúa únicamente como un bus de transporte asíncrono y diferido[cite: 4, 6].

---

## 2. Pipeline de Transmisión de Datos en 6 Etapas

[Etapa 1: Captura Local]
↓ Validaciones de dominio en cliente (horas, horómetros, montos)
[Etapa 2: Persistencia Inmediata en Disco]
↓ Inserción atómica en caché del SDK con UUID v4 (Confirmación <= 2 s)
[Etapa 3: Cola y Transmisión Asíncrona (FIFO)]
↓ Monitor de red detecta enlace y vacía mutaciones con backoff exponencial
[Etapa 4: Barrera de Seguridad Server-Side]
↓ Security Rules validan JWT, Custom Claim 'role', titularidad y schema
[Etapa 5: Procesamiento Serverless]
↓ Cloud Function 'procesarJornada' recalcula horas y acumula horómetro
[Etapa 6: Consumo Web Reactivo]
Plataforma React recibe cambios vía TLS/WebSockets en tiempo real

*(Flujo de pipeline de seis etapas definido en la arquitectura técnica de SILMAY[cite: 4, 6]).*

---

## 3. Concurrencia, Idempotencia y Prevención de Conflictos (ADR-0004 y ADR-0005)

1. **Aislamiento por Titularidad de Dispositivo:**
   * No existen conflictos de edición concurrente entre operadores[cite: 4]. Cada registro operativo (`jornada`, `despacho`, `gasto`) es generado y firmado por un único terminal móvil y un único usuario[cite: 4, 6].
2. **Inmutabilidad de Registros Cerrados:**
   * Una vez que una jornada se marca como `estado: 'cerrada'` en el móvil, queda estrictamente sellada[cite: 4, 6]. Las Security Rules de Firestore rechazan cualquier intento posterior de modificación de sus campos operativos[cite: 4].
3. **Idempotencia Estricta con UUID v4:**
   * Queda estrictamente prohibido usar IDs autoincrementales o `collection.add()`[cite: 4].
   * Al pulsar "Guardar" en la app móvil, el código genera un UUID v4 criptográfico y persiste el documento mediante `set()`[cite: 4, 5, 6]. Si la señal se corta durante el envío de la mutación y el SDK reintenta transmitir el paquete, Firestore sobrescribe el documento existente con datos idénticos sin generar jamás un registro duplicado (RNF-11)[cite: 4, 6].
4. **Campos Segregados en Validación Contable:**
   * Las Security Rules permiten al rol `contador` modificar únicamente los campos `['estadoValidacion', 'validadoPor', 'fechaValidacion', 'comentarioValidacion']`[cite: 4]. El contador jamás puede alterar cantidades, valores USD ni horómetros capturados en campo[cite: 4, 5, 6].

---

## 4. Ciclo de Vida y Transición de Estados de la Jornada

* **Estado 1: ABIERTA (Captura en Campo):**
  * Registrada por el operador al comenzar turno[cite: 4, 5, 6].
  * Permite añadir deducciones o novedades[cite: 4, 6].
  * Regla estricta: No se permite tener más de 1 jornada abierta simultáneamente por usuario (`MSG-14`)[cite: 4, 5, 6].
* **Estado 2: CERRADA (Inmutable en Móvil):**
  * El operador registra `horaSalida`, `horometroFinal` y observaciones[cite: 4, 6].
  * Se computan localmente `horasEfectivas` y `deltaHorometro`[cite: 4, 6].
  * Pasa a la cola local de sincronización diferida como documento de solo lectura[cite: 4, 6].
* **Estado 3: PROCESADA (Consolidada en Backend):**
  * Una vez transmitida a Firestore, la Cloud Function `procesarJornada` detecta el documento con `estado == 'cerrada'` y `procesada == false`[cite: 4, 6].
  * En una transacción ACID, la Cloud Function suma el `deltaHorometro` en el documento respectivo de `maquinaria`, genera notificaciones si `tieneFallaCritica == true` y marca `procesada = true`[cite: 4, 6]. Esto previene recálculos dobles ante reintentos de red[cite: 4, 6].

---

## 5. Control de Desconexión de Sesión (RNF-04)

* **Primer Inicio de Sesión:** Exige obligatoriamente conectividad para validar credenciales en Firebase Authentication y descargar Custom Claims (`MSG-02` si no hay enlace)[cite: 5, 6].
* **Persistencia de Sesión:** Firebase SDK persiste el refresh token y los catálogos maestros en disco local[cite: 4, 6]. La expiración del access token tras períodos prolongados sin red se refresca de forma transparente apenas se recupera el enlace[cite: 4].
* **Protección ante Cierre de Sesión Accidental:** Si el operador intenta cerrar sesión teniendo documentos con estado pendiente de sincronización en disco, la aplicación debe alertar al usuario y bloquear la acción para evitar la purga accidental de datos no respaldados[cite: 5, 6].

---

## 6. Métrica de Calidad: Tasa de Entregabilidad de Sincronización (TES)

Para auditar el cumplimiento del requerimiento **RNF-09** ($TES \ge 99.0\%$)[cite: 5, 6]:
* Cada documento contiene `createdAtClient` (timestamp local de origen) y `syncedAtServer` (serverTimestamp al tocar Firestore)[cite: 4, 6].
* La diferencia temporal entre ambos atributos permite calcular la latencia media de transporte diferido en las auditorías de post-test de la minera SILMAY[cite: 5, 6].