# Esquema Canónico de Cloud Firestore — Sistema SILMAY

Este documento define las 12 colecciones de Cloud Firestore en la región `southamerica-east1`, especificando sus atributos, tipos de datos, llaves primarias y reglas de integridad.

---

## 1. Reglas Globales de Integridad y Tipado

- **ID de Documento Mandatorio:** Todo documento generado en clientes móviles (`jornadas`, `despachos`, `gastos`) debe usar como Document ID un **UUID v4** generado en el dispositivo y guardarse con `.document(uuidV4).set(...)`. Prohibido el uso de `.add()`.
- **Campos de Control Obligatorios:** Las tres colecciones operativas (`jornadas`, `despachos`, `gastos`) deben contener obligatoriamente estos 6 atributos:
  - `uuid` (string): Identificador único universal coincidente con el ID del documento.
  - `createdAtClient` (timestamp local): Fecha y hora exacta del guardado en disco en el dispositivo.
  - `syncedAtServer` (FieldValue.serverTimestamp()): Fecha y hora de recepción en la nube.
  - `userId` (string): UID del usuario autenticado autor del registro.
  - `deviceId` (string): Identificador único del dispositivo móvil emisor.
  - `zona` (string): Zona operativa heredada de la asignación del usuario (`zonaAsignada`).

---

## 2. Inventario Canónico de las 12 Colecciones

### 2.1 Colección `usuarios`
Gestión de cuentas y perfiles autoritativos.
- `uid` (string, PK): Coincide con el UID de Firebase Authentication.
- `nombres` (string): Nombres y apellidos completos.
- `cedula` (string): Cédula ecuatoriana válida de 10 dígitos (única en el sistema).
- `telefono` (string): Teléfono de contacto (acceso restringido a Admin).
- `email` (string): Correo electrónico institucional único.
- `rol` (string enum): `'operador' | 'supervisor' | 'contador' | 'admin'` (asignado como Custom Claim).
- `tipoLicencia` (string | null): Tipo de licencia de conducir en caso de aplicar.
- `zonaAsignada` (string): Frente operativo asignado (ej. `'cantera-banos'`).
- `cargo` (string): Título o puesto de trabajo.
- `estado` (boolean): `true` (activo) o `false` (baja lógica / deshabilitado).

### 2.2 Colección `jornadas`
Registro de turnos de maquinaria pesada.
- `uuid` (string, PK): UUID v4.
- `maquinaId` (string): Llave foránea que referencia al documento en `maquinaria`.
- `lugarId` (string): Llave foránea hacia `catalogos/lugares/items`.
- `horaInicio` (timestamp): Registro de inicio de la jornada.
- `horaSalida` (timestamp | null): Registro de fin (obligatorio `horaSalida > horaInicio`).
- `horometroInicial` (number, 1 decimal): Lectura de horómetro al iniciar.
- `horometroFinal` (number, 1 decimal | null): Lectura final (`horometroFinal > horometroInicial`).
- `horasBrutas` (number, 1 decimal | null): `horaSalida - horaInicio`.
- `horasRestadas` (number, 1 decimal): Sumatoria total de tiempos improductivos.
- `horasEfectivas` (number, 1 decimal | null): `horasBrutas - horasRestadas`.
- `deltaHorometro` (number, 1 decimal | null): `horometroFinal - horometroInicial`.
- `observaciones` (string | null): Comentarios del turno.
- `estado` (string enum): `'abierta' | 'cerrada'` (cerrada es inmutable desde el móvil).
- `tieneFallaCritica` (boolean): `true` si al menos una deducción tiene motivo de falla con severidad crítica.
- `notificadaFalla` (boolean): `false` por defecto; Cloud Functions la marca `true` tras generar la alerta.
- `procesada` (boolean): `false` por defecto; Cloud Functions la marca `true` tras acumular el horómetro.
- `deducciones` (array of maps): Arreglo embebido de tiempos improductivos:
  - `id` (string): UUID v4 local.
  - `motivo` (string): ID o código del catálogo de motivos.
  - `duracionHoras` (number, 1 decimal): Duración mayor a cero.
  - `esFalla` (boolean): `true` si es falla mecánica/hidráulica/neumáticos.
  - `severidad` (string enum | null): `'leve' | 'moderada' | 'critica'`.
  - `descripcion` (string | null): Descripción detallada (mínimo 10 caracteres si es falla).
  - `coordenadas` (map | null): `{ lat: number, lng: number }` capturadas puntualmente.
- Campos de control: `userId`, `deviceId`, `zona`, `createdAtClient`, `syncedAtServer`.

### 2.3 Colección `despachos`
Registro offline de ventas y salidas de material pétreo.
- `uuid` (string, PK): UUID v4.
- `materialId` (string): Referencia a `catalogos/materiales/items`.
- `unidadMedida` (string enum): `'m3' | 'viajes'`.
- `cantidad` (number): Cantidad despachada (`> 0`).
- `clienteId` (string): Referencia a `catalogos/clientes/items`.
- `volquetaId` (string): Referencia a `maquinaria` donde `tipoEquipo == 'volqueta'`.
- `lugarId` (string): Frente de extracción de origen.
- `metodoCobro` (string enum): `'efectivo' | 'transferencia' | 'credito'`.
- `valorUSD` (number, 2 decimales): Monto total facturado o pactado (`> 0`).
- `fechaHora` (timestamp): Momento de la emisión.
- `estadoValidacion` (string enum): `'pendiente' | 'validado' | 'observado'`.
- `validadoPor` (string | null): UID del Contador que revisó el registro.
- `fechaValidacion` (timestamp | null): Fecha de la validación contable.
- `comentarioValidacion` (string | null): Justificación (obligatoria si es `'observado'`).
- Campos de control: `userId`, `deviceId`, `zona`, `createdAtClient`, `syncedAtServer`.

### 2.4 Colección `gastos`
Captura de egresos de campo.
- `uuid` (string, PK): UUID v4.
- `rubro` (string enum): `'combustible' | 'viáticos' | 'repuestos' | 'otros'`.
- `descripcion` (string | null): Detalle del gasto (obligatorio si `rubro == 'otros'`).
- `montoUSD` (number, 2 decimales): Monto positivo (`> 0`).
- `numeroComprobante` (string | null): Número de nota de venta o factura física.
- `unidadId` (string): Maquinaria o volqueta receptora del gasto.
- `fechaHora` (timestamp): Momento del desembolso.
- `estadoValidacion` (string enum): `'pendiente' | 'validado' | 'observado'`.
- `validadoPor` (string | null), `fechaValidacion` (timestamp | null), `comentarioValidacion` (string | null).
- Campos de control: `userId`, `deviceId`, `zona`, `createdAtClient`, `syncedAtServer`.

### 2.5 Colección `maquinaria`
Inventario unificado de equipos pesados y volquetas.
- `codigoInterno` (string, PK/Unique): Código de flota (ej. `'EX-01'`, `'VOL-03'`).
- `placa` (string | null, Unique): Placa vehicular (obligatoria en volquetas).
- `marca` (string), `modelo` (string).
- `tipoEquipo` (string enum): `'excavadora' | 'cargadora' | 'retroexcavadora' | 'tractocamión' | 'volqueta'`.
- `horometroBase` (number, 1 decimal): Horómetro con el que ingresó al sistema.
- `horometroActual` (number, 1 decimal): Horómetro acumulado actualizado por Cloud Functions o mantenimientos.
- `intervaloMantenimiento` (number): Horas de ciclo preventivo (ej. 250 o 500 horas).
- `proximoMantenimientoHoras` (number, 1 decimal): Umbral de parada técnica.
- `estado` (string enum): `'operativa' | 'requiere_mantenimiento' | 'inactiva'`.

### 2.6 Colección `mantenimientos`
Historial de servicios mecánicos preventivos o correctivos.
- `id` (string, PK): UUID v4.
- `maquinaId` (string): Referencia al documento en `maquinaria`.
- `fecha` (timestamp): Fecha del servicio ejecutado.
- `costoUSD` (number, 2 decimales): Valor total del mantenimiento.
- `repuestos` (array of strings): Lista de partes o consumibles sustituidos.
- `horometroServicio` (number, 1 decimal): Horómetro al momento del servicio.
- `nuevoLimite` (number, 1 decimal): `horometroServicio + intervaloMantenimiento` (se actualiza en `maquinaria.proximoMantenimientoHoras`).
- `realizadoPor` (string): Responsable del taller o mecánico.

### 2.7 Colección `notificaciones`
Alertas generadas automáticamente por Cloud Functions.
- `id` (string, PK): ID determinista generado en backend (`falla_{jornadaUuid}` o `maint_{maquinaId}_{horometro}`).
- `tipo` (string enum): `'falla_critica' | 'mantenimiento'`.
- `referenciaId` (string): ID de la jornada o de la maquinaria causante.
- `mensaje` (string): Descripción técnica de la alerta.
- `severidad` (string enum): `'advertencia' | 'critica'`.
- `fecha` (timestamp): Momento de emisión.
- `atendida` (boolean): `false` por defecto.
- `atendidaPor` (string | null): UID del Administrador que atendió la alerta.

### 2.8 Colección `conciliaciones`
Cierres mensuales de exactitud contable.
- `periodo` (string, PK): Formato `'YYYY-MM'`.
- `tipo` (string enum): `'despachos' | 'gastos'`.
- `registrosValidados` (number): Total de registros aprobados en el sistema.
- `eventosReales` (number): Conteo extraído de la bitácora física.
- `pReg` (number, 2 decimales): `(registrosValidados / eventosReales) * 100`.
- `contadorId` (string): UID del Contador actuante.
- `fecha` (timestamp): Momento de la conciliación.

### 2.9 Colección `consolidados`
Reportes gerenciales mensuales generados por Cloud Functions.
- `id` (string, PK): Formato `'periodo_YYYY_MM'`.
- `periodoInicio` (timestamp), `periodoFin` (timestamp).
- `totalesMaterial` (map of numbers): Sumatoria de m3/viajes por tipo de material.
- `ingresosPorCliente` (map of numbers): Facturación por cliente.
- `ingresosPorMetodo` (map of numbers): Desglose por efectivo, transferencia y crédito.
- `horasPorEquipo` (map of numbers): Horas acumuladas por máquina.
- `horasPorOperador` (map of numbers): Horas acumuladas por operador.
- `horasRestadasPorMotivo` (map of numbers): Tiempos improductivos clasificados.
- `gastosPorRubro` (map of numbers): Desglose financiero de egresos.
- `resultadoOperativo` (number, 2 decimales): Ingresos totales - Gastos totales.
- `generadoEn` (timestamp), `generadoPor` (string).
- `estado` (string enum): `'borrador' | 'aprobado'`.
- `aprobadoPor` (string | null), `fechaAprobacion` (timestamp | null).

### 2.10 Colección `catalogos/{tipo}/items`
Catálogos maestros del sistema (tipos: `materiales`, `clientes`, `lugares`, `rubros`, `motivos`).
- `id` (string, PK): Slug o ID único.
- `nombre` (string, Unique por tipo): Denominación del elemento.
- `activo` (boolean): `true` (vigente) o `false` (baja lógica; prohibido borrar registros).

### 2.11 Colección `parametros`
Configuraciones globales del sistema (documento único `sistema`).
- `umbralProximoMantenimiento` (number, rango 0.0 - 1.0): Por defecto `0.90`.
- `zonaHoraria` (string): Por defecto `'America/Guayaquil'`.

### 2.12 Colección `auditoria`
Bitácora inmutable append-only gestionada exclusivamente por Cloud Functions.
- `id` (string, PK): ID autogenerado.
- `accion` (string): `'CREATE' | 'UPDATE' | 'PERMISSION_DENIED'`.
- `coleccion` (string): Nombre de la colección afectada.
- `documentoId` (string): ID del documento involucrado.
- `usuarioId` (string | null): UID del emisor de la petición.
- `detalle` (map): Metadatos adicionales o diff del cambio.
- `fecha` (FieldValue.serverTimestamp()): Marca de tiempo inalterable del servidor.

---

## 3. Índices Compuestos Mandatorios (`firestore.indexes.json`)

Para garantizar que las consultas web reactivas y los filtros multicriterio no fallen ni ejecuten escaneos globales:

1. **`jornadas`:** `userId` (ASC), `createdAtClient` (DESC)
2. **`despachos`:** `estadoValidacion` (ASC), `fechaHora` (DESC)
3. **`despachos`:** `clienteId` (ASC), `fechaHora` (DESC)
4. **`gastos`:** `rubro` (ASC), `fechaHora` (DESC)
5. **`jornadas`:** `maquinaId` (ASC), `horaInicio` (DESC)