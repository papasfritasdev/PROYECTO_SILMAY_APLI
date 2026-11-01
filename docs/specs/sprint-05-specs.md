# Especificación Funcional y Técnica — Sprint 5 (27 Puntos)

- **Documento Fuente:** Resumen Técnico Pila del Producto SILMAY v2.0
- **Objetivo del Sprint:** Monitoreo web, validación contable, consolidación, exportación y pruebas ISO 25010.
- **Capacidad del Sprint:** 27 Puntos de Historia (de un total del backlog general de 128 pts: 76 HU + 52 HT).
- **Entregables:** HT-10, HT-11, HU-15, HU-12, HU-13, HU-14, HU-16.

---

## 1. Inventario de Historias del Sprint 5

| ID | Tipo | Título | Pts | Responsabilidad Principal |
| :--- | :--- | :--- | :--- | :--- |
| **HT-10** | Arquitectura | Cloud Function de consolidación | 5 | Función callable `consolidarPeriodo` (≤ 60 s para 5 000 docs) y cálculo de Resultado Operativo. |
| **HT-11** | Calidad / QA | Pruebas de desempeño ISO 25010 | 3 | Instrumentación Profiler/Performance, carga (20 móviles + 5 web) y métricas TES, $T_{cons}$, $P_{reg}$. |
| **HU-15** | Historia Usuario | Monitoreo y filtrado operativo | 3 | Grillas reactivas en tiempo real con filtros combinados y alerta `MSG-10` sin resultados. |
| **HU-12** | Historia Usuario | Consolidación gerencial del período | 5 | Generación de reporte mensual, cálculo de márgenes, alerta `MSG-08` y aprobación exclusiva de Admin. |
| **HU-13** | Historia Usuario | Validación y conciliación contable | 5 | Aprobación/observación contable (`MSG-15`), inmutabilidad de datos de campo y cálculo de $P_{reg}$. |
| **HU-14** | Historia Usuario | Exportación de reportes a Excel y PDF | 3 | Descarga de `.xlsx` por hojas separadas y `.pdf` maquetado con membrete y firmas. |
| **HU-16** | Historia Usuario | Gestión de catálogos y parámetros | 3 | ABM de catálogos maestros y umbral preventivo, control de duplicados (`MSG-16`) y baja lógica. |

---

## 2. Detalle de Historias Técnicas

### HT-10: Cloud Function de consolidación (5 pts) [RF-25, RF-26, RNF-02]
- **Alcance:**
  1. Implementación de la función HTTPS callable `consolidarPeriodo.ts` restringida autoritativamente a los roles `admin` y `contador`.
  2. Agregación optimizada por rango de fechas (`periodoInicio` a `periodoFin`) capaz de procesar hasta 5 000 documentos operativos en $\le 60\text{ segundos}$ (RNF-02) apoyándose en los índices compuestos de `firestore.indexes.json`.
  3. Totalización en mapas estructurados: `totalesMaterial`, `ingresosPorCliente`, `ingresosPorMetodo`, `horasPorEquipo`, `horasPorOperador`, `horasRestadasPorMotivo` y `gastosPorRubro`.
  4. Cálculo del $\text{Resultado Operativo} = \sum \text{Ingresos} - \sum \text{Gastos}$ y persistencia del documento en la colección `consolidados` con `estado: 'borrador'` y marca `generadoEn` (para medir $T_{cons}$).

### HT-11: Pruebas de desempeño ISO 25010 (3 pts) [RNF-01 a 04, RNF-09]
- **Alcance:**
  1. Instrumentación de la aplicación Android con Firebase Performance Monitoring y Android Profiler para verificar en campo:
     - **CT-1:** Percentil 95 de confirmación de guardado local $\le 2.0\text{ s}$ (RNF-01).
     - **UR:** Consumo de memoria RAM $\le 300\text{ MB}$ y drenaje de batería $\le 15\%$ en una jornada continua de 8 horas (RNF-04).
  2. Ejecución de pruebas de concurrencia simulando 20 terminales móviles sincronizando simultáneamente y 5 sesiones web administrativas activas (RNF-03).
  3. Extracción de métricas para la evaluación del piloto Post-test en SILMAY:
     - $TES = (\text{Reportes sincronizados} / \text{Reportes generados en campo}) \times 100 \ge 99.0\%$.
     - $T_{cons}$ (Tiempo de consolidación) y $P_{reg}$ (Precisión del registro operativo).

---

## 3. Especificación de Historias de Usuario (Formato Gherkin)

### HU-15: Monitoreo y filtrado operativo (3 pts) — Rol: Contador / Admin

#### CA1: Filtrado multicriterio combinado
- **Dado** que el Contador o Administrador accede a las grillas de monitoreo de jornadas, despachos o gastos en la plataforma web,
- **Cuando** aplica filtros combinados por rango de fechas/mes, cliente, máquina, operador, frente de trabajo, material o `estadoValidacion`,
- **Entonces** la tabla muestra únicamente los registros que satisfacen todos los criterios seleccionados (RF-22).

#### CA2: Alerta ante búsqueda sin coincidencias
- **Dado** que el usuario aplica una combinación de filtros en la grilla web,
- **Cuando** la consulta a Firestore no devuelve ningún documento coincidente,
- **Entonces** el sistema despliega en pantalla exactamente el mensaje:
  > `"No existen movimientos que coincidan con los filtros seleccionados."` (`MSG-10`)

#### CA3: Incorporación reactiva en tiempo real
- **Dado** que el Contador o Administrador mantiene abierta la grilla de monitoreo,
- **Cuando** un operador en campo recupera conexión y su dispositivo sincroniza un nuevo despacho, gasto o jornada que coincide con el filtro activo,
- **Entonces** la fila se incorpora automáticamente a la tabla en tiempo real sin recargar la página (RF-22).

---

### HU-12: Consolidación gerencial del período (5 pts) — Rol: Administrador / Contador

#### CA1: Generación de consolidado en tiempo acotado
- **Dado** que el Administrador o Contador selecciona un período de fechas con hasta 5 000 registros operativos y solicita generar el consolidado,
- **Cuando** se ejecuta la Cloud Function `consolidarPeriodo`,
- **Entonces** el sistema procesa las colecciones y presenta el resumen gerencial completo en pantalla en $\le 60\text{ segundos}$ (RNF-02).

#### CA2: Cálculo exacto del Resultado Operativo
- **Dado** que existen despachos y gastos registrados en el período consultado,
- **Cuando** concluye la consolidación,
- **Entonces** el sistema calcula y exhibe el $\text{Resultado Operativo} = \sum \text{Ingresos (valorUSD)} - \sum \text{Gastos (montoUSD)}$ junto con los desgloses por material, cliente, equipo y rubro (RF-25, RF-26).

#### CA3: Alerta por período sin movimientos operativos
- **Dado** que el usuario solicita generar un consolidado para un rango de fechas en el que no existen jornadas, despachos ni gastos,
- **Cuando** se ejecuta la consulta,
- **Entonces** el sistema interrumpe la generación y muestra exactamente el mensaje:
  > `"No se encontraron despachos ni movimientos operativos registrados en el período seleccionado."` (`MSG-08`)

#### CA4: Aprobación ejecutiva exclusiva del Administrador
- **Dado** que se ha generado un documento en `consolidados` con `estado: 'borrador'`,
- **Cuando** el usuario con rol `admin` presiona el botón "Aprobar Consolidado",
- **Entonces** el documento cambia a `estado: 'aprobado'`, registra `aprobadoPor` (UID del Admin) y `fechaAprobacion`, quedando inmutable (RF-27).

---

### HU-13: Validación y conciliación contable (5 pts) — Rol: Contador

#### CA1: Marcado de registros como Validado sin alterar datos de campo
- **Dado** que el Contador revisa un despacho o un gasto en estado `'pendiente'`,
- **Cuando** aprueba la transacción marcándola como `'validado'`,
- **Entonces** el sistema actualiza únicamente `estadoValidacion: 'validado'`, `validadoPor` (UID del Contador) y `fechaValidacion`, manteniendo intactos los montos, cantidades y datos originales capturados en mina (RF-24).

#### CA2: Comentario obligatorio al marcar como Observado
- **Dado** que el Contador detecta una inconsistencia en un despacho o gasto y selecciona el estado `'observado'`,
- **Cuando** intenta guardar sin escribir una justificación en `comentarioValidacion`,
- **Entonces** el sistema bloquea la acción y muestra exactamente el mensaje:
  > `"Registre un comentario para marcar el registro como Observado."` (`MSG-15`)

#### CA3: Cálculo automático de la métrica de precisión ($P_{reg}$)
- **Dado** que el Contador realiza el cierre de conciliación de un período (`YYYY-MM`) ingresando el conteo de `eventosReales` extraído de la bitácora física de la empresa,
- **Cuando** guarda la conciliación en la colección `conciliaciones`,
- **Entonces** el sistema calcula y persiste automáticamente el indicador:
  $$P_{reg} = \left( \frac{\text{registrosValidados}}{\text{eventosReales}} \right) \times 100$$

#### CA4: Bloqueo server-side de edición sobre campos operativos
- **Dado** que el Contador intenta modificar campos operativos de un despacho o gasto (como `valorUSD`, `cantidad`, `montoUSD` o `materialId`),
- **Cuando** la petición llega a Firestore,
- **Entonces** la regla `affectedKeys().hasOnly(['estadoValidacion', 'validadoPor', 'fechaValidacion', 'comentarioValidacion'])` en `firestore.rules` rechaza la escritura (ADR-0005, RF-24).

---

### HU-14: Exportación de reportes a Excel y PDF (3 pts) — Rol: Contador / Admin

#### CA1: Exportación modular a archivo Excel (.xlsx)
- **Dado** que el Contador o Administrador visualiza un reporte operativo o un consolidado gerencial,
- **Cuando** selecciona "Exportar a Excel",
- **Entonces** el navegador descarga un archivo `.xlsx` con pestañas separadas para jornadas, despachos, gastos y totales cuyos valores coinciden exactamente con los exhibidos en pantalla (RF-28).

#### CA2: Exportación maquetada a archivo PDF (.pdf)
- **Dado** que el usuario requiere un documento formal para sustento administrativo o contable,
- **Cuando** selecciona "Exportar a PDF",
- **Entonces** se descarga un archivo `.pdf` maquetado con el membrete de SILMAY, rango del período, tablas de resumen y bloques para firmas de responsabilidad (RF-28).

#### CA3: Inclusión de trazabilidad contable en los reportes
- **Dado** que se genera un reporte detallado de despachos o gastos en Excel o PDF,
- **Cuando** se listan las transacciones individuales,
- **Entonces** cada fila incluye obligatoriamente el número de comprobante, método de cobro y su `estadoValidacion` actual.

---

### HU-16: Gestión de catálogos y parámetros (3 pts) — Rol: Administrador

#### CA1: Sincronización de nuevos ítems hacia clientes móviles
- **Dado** que el Administrador agrega un nuevo ítem en `catalogos/{tipo}/items` (`materiales`, `clientes`, `lugares`, `rubros` o `motivos`) con `activo: true` o ajusta el `umbralProximoMantenimiento` en `parametros`,
- **Cuando** los dispositivos móviles se conectan a Internet,
- **Entonces** los nuevos valores aparecen automáticamente en los selectores de la app Android sin requerir actualizar el APK (RF-32).

#### CA2: Bloqueo de valores duplicados en catálogos maestros
- **Dado** que el Administrador intenta crear un nuevo registro dentro de un catálogo (`catalogos/{tipo}/items`),
- **Cuando** el `nombre` ingresado ya existe dentro de ese mismo tipo de catálogo,
- **Entonces** el sistema impide el guardado y muestra exactamente el mensaje:
  > `"Ya existe un registro con ese valor en el catálogo."` (`MSG-16`)

#### CA3: Desactivación mediante baja lógica sin borrado físico
- **Dado** que un material, cliente, lugar o motivo ya no debe utilizarse en futuras operaciones de campo,
- **Cuando** el Administrador lo da de baja desde la web,
- **Entonces** el sistema cambia su atributo a `activo: false` (ocultándolo de los formularios móviles nuevos) sin eliminar físicamente el documento de Firestore para no romper las referencias históricas en reportes pasados (RF-32).