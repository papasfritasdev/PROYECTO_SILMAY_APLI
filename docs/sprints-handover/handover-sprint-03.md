# Informe de Relevo Técnico — Sprint 3: Captura en Campo Offline-First (Jornadas, Despachos y Sincronización Diferida)

- **Fecha de Cierre:** 17/11/2026
- **Desarrollador Responsable:** Bryan (`papasfritasdev`)
- **Asistente IA Utilizado:** Google Antigravity
- **Puntos de Historia Completados:** 26 / 26 Pts (HU-07 [5 pts], HU-04 [5 pts], HU-03 [8 pts], HU-08 [8 pts]) + Saneamiento de CI y Kover (Fase 0)

---

## 1. Historias Implementadas y Estado

| ID | Tipo | Título | Pts | Estado | Criterios Gherkin Cumplidos |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Fase 0** | Técnica | Saneamiento de CI, placeholder dinámico y Kover | 0 | Completada | Verificación estricta de rutas CI, umbral Kover $\ge 70\%$ en `domain` |
| **HU-07** | Usuario | Uso de la aplicación sin conexión | 5 | Completada | CA1 (login inicial con red), CA2 (catálogos en modo avión), CA3 (bloqueo logout con pendientes `MSG-16`), CA4 (refresco reactivo) |
| **HU-04** | Usuario | Registro de jornada de trabajo | 5 | Completada | CA1 (apertura sugerida), CA2 (cierre automático $H_{brutas}$, $H_{efectivas}$, $\Delta H$), CA3 (validaciones `MSG-05`, `MSG-06`, `MSG-11`, `MSG-12`), CA4 (jornada única `MSG-14`) |
| **HU-03** | Usuario | Registro de venta y despacho de pétreos | 8 | Completada | CA1 (guardado offline $\le 2$ s con `pendiente`), CA2 (validación háptica/visual `MSG-07`), CA3 (manejo de memoria `MSG-04`), CA4 (UUID v4 + metadatos) |
| **HU-08** | Usuario | Sincronización automática de registros | 8 | Completada | CA1 (reanudación automática reactiva con monitor de red), CA2 (idempotencia `set()` sin duplicados), CA3 (auditoría de rechazos), CA4 (trazabilidad métrica $TES \ge 99.0\%$) |

---

## 2. Mapa de Archivos Clave Creados o Modificados

### 2.1 Fase 0: CI y Métricas de Cobertura
- `.github/workflows/ci.yml`: Rutas corregidas para comprobación en subdirectorios (`./gradlew`, `package.json`), inyección en tiempo de ejecución del placeholder `google-services.json` sin versionar secretos, y activación de `koverVerify` y `koverHtmlReport`.
- `android/build.gradle.kts`: Integración del plugin `org.jetbrains.kotlinx.kover` versión `0.9.1`.
- `android/app/build.gradle.kts`: Configuración de reglas Kover limitadas al paquete `ec.silmay.app.domain.*` con umbral mínimo obligatorio del 70%.

### 2.2 Capa Dominio Android (`android/app/src/main/java/ec/silmay/app/domain/`) — Kotlin Puro
- **Modelos:**
  - `model/ItemCatalogo.kt`: Entidad para materiales, clientes y lugares.
  - `model/TipoCatalogo.kt`: Enumerador (`MATERIALES`, `CLIENTES`, `LUGARES`).
  - `model/Maquinaria.kt`: Entidad para flota de maquinaria pesada.
  - `model/Jornada.kt`: Entidad de turno operativo (`id`, `maquinaId`, `lugarId`, `frenteExtraccion`, `horometros`, `horas`, `deducciones`, etc.).
  - `model/Deduccion.kt` y `model/SeveridadFalla.kt`: Registro de paradas y fallas mecánicas con severidad (`LEVE`, `MODERADA`, `CRITICA`).
  - `model/EstadoJornada.kt`: Estados del ciclo de vida (`ABIERTA`, `CERRADA`).
  - `model/Despacho.kt`: Entidad para despachos pétreos (`id`, `materialId`, `cantidad`, `unidad`, `metodoCobro`, `valorUSD`, `frenteExtraccion`, etc.).
  - `model/UnidadMedida.kt`: `M3`, `TONELADA`, `VIAJE`.
  - `model/MetodoCobro.kt`: `EFECTIVO`, `CREDITO`, `TRANSFERENCIA`.
  - `model/EstadoValidacion.kt`: `PENDIENTE`, `APROBADO`, `RECHAZADO`.
- **Motor Matemático:**
  - `usecase/CalculadoraJornada.kt`: Implementación canónica de fórmulas oficiales de `docs/business-formulas.md` ($H_{brutas}$, $\Delta H$, $H_{efectivas}$, validaciones de horas, horómetros y deducciones). Incluye piso de $0.1$ h para jornadas de demostración/prueba cerradas inmediatamente y evaluación estricta de `MSG-12` condicionada a la existencia de deducciones registradas.
- **Contratos de Repositorio:**
  - `repository/CatalogoRepository.kt`: Lectura offline de catálogos y maquinaria.
  - `repository/SincronizacionRepository.kt`: Contrato para conteo y observación reactiva de registros pendientes.
  - `repository/JornadaRepository.kt`: Contrato para apertura, cierre y consulta de jornadas activas.
  - `repository/DespachoRepository.kt`: Contrato para registro de ventas y despachos.
  - `repository/NetworkMonitor.kt`: Contrato desacoplado para observación del estado de conectividad (`Flow<Boolean>`).
- **Casos de Uso:**
  - `usecase/CerrarSesionUseCase.kt`: Protección ante cierre de sesión con pendientes (`CerrarSesionResultado.BloqueadoPorPendientes`).
  - `usecase/IniciarJornadaUseCase.kt`: Apertura con validación de jornada activa (`MSG-14`), sugerencia de horómetro y generación de UUID v4.
  - `usecase/CerrarJornadaUseCase.kt`: Cierre con cálculo matemático, detección de fallas críticas y persistencia local atómica.
  - `usecase/RegistrarDespachoUseCase.kt`: Validación de cantidades/montos (`MSG-07`), herencia de frente de extracción activo y guardado local.

### 2.3 Capa Datos Android (`android/app/src/main/java/ec/silmay/app/data/`)
- `repository/CatalogoRepositoryImpl.kt`: Consulta Offline-First sobre Cloud Firestore con lectura inicial de caché local (`Source.CACHE`) y fallback automático a servidor/emulador (`Source.DEFAULT`) si la base local SQLite está vacía en el primer arranque.
- `repository/SincronizacionRepositoryImpl.kt`: Implementación sobre `BitacoraPendientes` con `contarPendientes()` y `observarPendientes(): Flow<Int>`.
- `repository/JornadaRepositoryImpl.kt`: Coordinación con `RegistroOperativoRepository` para almacenamiento en colección `jornadas` con `.document(uuid).set(...)`.
- `repository/DespachoRepositoryImpl.kt`: Coordinación con `RegistroOperativoRepository` para almacenamiento en colección `despachos`.
- `sync/BitacoraPendientes.kt`: Bitácora basada en `SharedPreferences` con soporte reactivo vía `callbackFlow` de `OnSharedPreferenceChangeListener`.
- `sync/ConnectivityNetworkMonitor.kt`: Implementación reactiva basada en `ConnectivityManager.NetworkCallback` y `callbackFlow` para emitir cambios online/offline.
- `sync/ConciliadorSincronizacion.kt`: Conciliador conectado al `NetworkMonitor` para solicitar sincronización automática al recuperar red.

### 2.4 Capa Presentación Android (`android/app/src/main/java/ec/silmay/app/presentation/`)
- `sesion/SesionViewModel.kt`: Expone reactivamente `esOnline`, `pendientesSync` e intercepta el cierre de sesión mediante `CerrarSesionUseCase`.
- `menu/MenuPrincipalScreen.kt`: Badges visuales de conectividad (`En línea` / `Sin conexión`) y estado de sincronización (`✓ Todo sincronizado` / `⏳ X pendiente(s)`), además de diálogo bloqueante `AlertDialog` ante cierre de sesión con pendientes.
- `jornada/JornadaViewModel.kt`: Manejo de estado de apertura y cierre de jornada, cálculo en vivo y feedback visual.
- `jornada/AperturaJornadaScreen.kt`: Pantalla de inicio de jornada con selección de máquina, lugar, hora, horómetro sugerido y botón secundario "Cancelar" para abortar el turno sin persistencia.
- `jornada/CierreJornadaScreen.kt`: Pantalla de cierre con desglose de deducciones, captura precisa de hora de salida al pulsar el botón, distribución flexible de ancho para evitar deformación del botón "+ Deducción", validaciones en tiempo real y bloqueo tras cierre.
- `despacho/DespachoViewModel.kt`: Manejo de estado del formulario de despacho, validaciones y feedback háptico.
- `despacho/DespachoScreen.kt`: Pantalla de captura rápida de despachos con soporte para feedback háptico (`LocalHapticFeedback`).
- `navigation/SilmayNavHost.kt`: Rutas de navegación integradas (`apertura_jornada`, `cierre_jornada`, `despacho`) y cableado con `MenuPrincipalScreen`.

### 2.5 Inyección de Dependencias
- `di/SyncModule.kt`: Provisión de `NetworkMonitor` (`ConnectivityNetworkMonitor`) y enlace con `ConciliadorSincronizacion`.
- `di/RepositoryModule.kt`: Provisión de `CatalogoRepository`, `SincronizacionRepository`, `JornadaRepository` y `DespachoRepository`.

---

## 3. Contratos de Datos y Esquema Firestore

- **Colección `jornadas`:**
  - Identificador: `uuid` (UUID v4 generado en cliente).
  - Campos: `maquinaId`, `lugarId`, `frenteExtraccion`, `horaInicio`, `horaFin`, `horometroInicial`, `horometroFinal`, `deltaHorometro`, `horasBrutas`, `horasEfectivas`, `deducciones` (array), `tieneFallaCritica`, `estado` (`'abierta'` / `'cerrada'`), `procesada`.
  - Metadatos: `uuid`, `createdAtClient`, `syncedAtServer`, `userId`, `deviceId`, `zona`.
- **Colección `despachos`:**
  - Identificador: `uuid` (UUID v4 generado en cliente).
  - Campos: `materialId`, `unidadMedida`, `cantidad`, `valorUSD`, `metodoCobro`, `clienteId`, `volqueta`, `lugarId`, `frenteExtraccion`, `estadoValidacion` (`'pendiente'`), `fecha`.
  - Metadatos: `uuid`, `createdAtClient`, `syncedAtServer`, `userId`, `deviceId`, `zona`.
- **Colección `catalogos` y `maquinaria`:**
  - Lectura offline-first con soporte de caché persistente de Firestore (`Source.CACHE`).
- **Garantía de Idempotencia:**
  - Toda mutación usa `document(uuid).set(...)`. Prohibido el uso de `.add()`. Los reintentos de red del SDK no generan registros duplicados (RNF-11).

---

## 4. Auditoría de Calidad y Definición de Terminado (DoD)

- [x] **Criterios Gherkin Verificados:** HU-07 (CA1–CA4), HU-04 (CA1–CA4), HU-03 (CA1–CA4), HU-08 (CA1–CA4).
- [x] **Cobertura de Pruebas Unitarias en Dominio:** Kover configurado y validado en Android con umbral $\ge 70.0\%$ (`koverVerify` exitoso). Cobertura en Web: 100% en `domain`.
- [x] **Clean Architecture Estricta:** La capa `domain` en Android (`ec.silmay.app.domain`) contiene Kotlin puro con 0 imports de `android.*`, `androidx.*` o `com.google.firebase.*`.
- [x] **Catálogo Canónico de Mensajes:** Uso estricto de identificadores `MSG-01` a `MSG-18` según `docs/messages-catalog.md`.
- [x] **Seguridad y Secretos:** Cero credenciales en Git; placeholder generado dinámicamente en CI; `android:allowBackup="false"` y reglas de exclusión de datos configuradas.
- [x] **Resultados de Ejecución de Pruebas en el Monorepo:**

| Módulo / Suite | Comando | Pruebas Exitosas | Fallos | Cobertura Dominio |
| :--- | :--- | :--- | :--- | :--- |
| **Android Unit Tests + Kover** | `cd android; .\gradlew testDebugUnitTest koverVerify` | **134 / 134** | 0 | $\ge 70\%$ (Aprobado) |
| **Firebase Functions** | `cd firebase/functions; npm test` | **35 / 35** | 0 | N/A |
| **Web Platform (Vitest)** | `cd web; npm test` | **110 / 110** | 0 | **100%** |
| **Total General Monorepo** | | **279 / 279** | **0** | **100% Verde** |

---

## 5. Puntos de Integración y Advertencias para el Siguiente Sprint (Sprint 4)

### 5.1 Capacidades Listas para Consumo en el Sprint 4
1. **HU-06 (Registro de Gastos Diarios y Combustible):**
   - La arquitectura de persistencia offline (`GuardarRegistroOperativoUseCase`, `RegistroOperativoRepository`, `ColeccionOperativa.GASTOS`) está 100% lista para ser consumida por el nuevo caso de uso `RegistrarGastoUseCase`.
   - La bitácora reactiva de sincronización (`BitacoraPendientes`) y los badges de conectividad en `MenuPrincipalScreen` reflejarán automáticamente los gastos pendientes sin modificaciones en la infraestructura de sync.
2. **HU-05 (Registro de Novedades y Fallas Mecánicas):**
   - El modelo de dominio `Deduccion` y `SeveridadFalla` implementado en `HU-04` servirá de base directa para vincular novedades operativas con el historial de la maquinaria y alertas preventivas.
3. **HT-09 / HU-10 / HU-11 (Mantenimiento, Horómetros y Alertas):**
   - Las métricas canónicas calculadas en `CalculadoraJornada` ($\Delta H$, $H_{efectivas}$) alimentan directamente las reglas del semáforo preventivo y cálculo del próximo mantenimiento.

### 5.2 Advertencias Técnicas y Recomendaciones ("Gotchas")
- **Bitácora de Pendientes en Pruebas Unitarias:** Al probar componentes reactivos (`Flow` / corrutinas) que observan `NetworkMonitor` o `BitacoraPendientes`, asegurarse de inicializar los emisores con `replay = 1` o correr sobre `backgroundScope` para evitar pérdida de emisiones antes de que el collector esté suscrito.
- **Compose Material 3 Deprecations:** Evitar usar `Modifier.menuAnchor()` sin argumentos en Compose M3 1.3+; utilizar `Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)`.
- **Validación de Formatos en Emuladores:** El SDK de Functions en Android requiere que la clave en `google-services.json` cumpla con el formato sintáctico (`AIza` seguido de 35 caracteres alfanuméricos) incluso al comunicarse contra los emuladores locales (`10.0.2.2`).
- **Comportamiento de Caché Inicial de Firestore en SQLite:** Al consultar `Source.CACHE` en un dispositivo o emulador recién instalado, Firestore devuelve un `QuerySnapshot` con `size = 0` (no arroja excepción). Por ello, `CatalogoRepositoryImpl` valida si `cacheSnapshot.documents.isEmpty()` para invocar `Source.DEFAULT` y poblar la base local.
- **Tolerancia para Pruebas Rápidas de Jornadas (Demos):** Al abrir y cerrar un turno en lapsos de segundos (< 3 min), el redondeo a 1 decimal calcula $0.0$ h brutas. Se estableció una cota mínima de $0.1$ h (mínima unidad de reporte minero) y se condicionó `MSG-12` a `deducciones.isNotEmpty()` para evitar falsos positivos al validar $H_{restadas} \ge H_{brutas}$.
- **Imposibilidad Legal de Borrado en Auditoría Minera:** Por reglas de seguridad y auditoría minera (`firestore.rules` con `allow delete: if false`), las jornadas y despachos no se pueden eliminar de la base de datos. Los turnos abiertos por error en producción deben cerrarse inmediatamente con la observación "Iniciada por error", o bien descartarse antes de guardar usando el botón "Cancelar".

---

## 6. Protocolo de Relevo Humano (Walkthrough)

1. El desarrollador saliente (Bryan) realiza el merge formal de `feature/sp3-captura-offline` a `develop` una vez verificado el pipeline de CI.
2. El desarrollador entrante (Alejandro):
   - Ejecuta `git pull origin develop`.
   - Ejecuta la suite de pruebas unitarias en Android: `cd android; .\gradlew testDebugUnitTest koverVerify`.
   - Verifica la ejecución limpia de pruebas en Web y Functions: `cd web; npm test` y `cd firebase/functions; npm test`.
3. Ambos firman la conformidad del Sprint 3 e inician la planificación del Sprint 4.
