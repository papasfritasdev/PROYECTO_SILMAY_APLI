# Fórmulas de Negocio y Métricas Técnicas — Sistema SILMAY

Este documento especifica la lógica ejecutable, cálculos matemáticos, formatos numéricos y métricas de calidad que rigen los módulos móvil, web y serverless.

---

## 1. Módulo M1: Control de Jornada de Maquinaria Pesada (RF-09)

### 1.1 Fórmulas Temporales y Mecánicas
- **Horas Brutas:** Duración total transcurrida entre el inicio y el fin de la jornada.
  $$\text{Horas Brutas} = \text{Hora\_Salida} - \text{Hora\_Inicio}$$
- **Horas Efectivas:** Tiempo real de trabajo productivo humano tras descontar tiempos improductivos (almuerzos, traslados, clima, fallas).
  $$\text{Horas Efectivas} = \text{Horas\_Brutas} - \sum \text{Horas\_Restadas}$$
- **Delta Horómetro ($\Delta$):** Desgaste mecánico real del motor del equipo durante el turno.
  $$\Delta\text{Horómetro} = \text{Horómetro\_Final} - \text{Horómetro\_Inicial}$$

### 1.2 Reglas de Validación Aritmética
1. $\text{Hora\_Salida} > \text{Hora\_Inicio}$ (Si no cumple: emitir `MSG-11`).
2. $\text{Horómetro\_Final} > \text{Horómetro\_Inicial}$ (Si no cumple: emitir `MSG-05`).
3. $\sum \text{Horas\_Restadas} < \text{Horas\_Brutas}$ (Si no cumple: emitir `MSG-12`).
4. Cada deducción individual debe tener una duración $> 0$ con máximo 1 decimal.

---

## 2. Módulo M8: Control de Maquinaria y Semáforo Preventivo (RF-29, RF-31)

### 2.1 Semáforo de Estado de Flota
El estado visual y operativo de cada máquina en la plataforma web se determina comparando el `horometroActual` contra el `proximoMantenimientoHoras` programado, utilizando un umbral de advertencia configurable (valor por defecto: 90% o 0.90 del ciclo):

- **Horas Remanentes:**
  $$\text{horasRestantes} = \text{proximoMantenimientoHoras} - \text{horometroActual}$$
- **Verde (Óptimo / Operativa):**
  $$\text{horasRestantes} > (\text{intervaloMantenimiento} \times (1 - \text{umbral}))$$
- **Amarillo (Próximo a Mantenimiento):**
  $$0 < \text{horasRestantes} \le (\text{intervaloMantenimiento} \times (1 - \text{umbral}))$$
- **Rojo (Mantenimiento Vencido / Requiere Mantenimiento):**
  $$\text{horasRestantes} \le 0 \quad (\text{es decir, } \text{horometroActual} \ge \text{proximoMantenimientoHoras})$$

### 2.2 Reajuste tras Mantenimiento Realizado (RF-31)
Al registrar la ejecución formal de un mantenimiento, se calcula de forma automática el nuevo umbral y se reestablece la máquina al estado `operativa` (verde):
$$\text{proximoMantenimientoHoras} = \text{horometroServicio} + \text{intervaloMantenimiento}$$

---

## 3. Módulos M5 y M6: Conciliación Contable y Consolidación Gerencial

### 3.1 Métrica de Precisión del Registro Operativo ($P_{reg}$) (RF-24)
Instrumentada en la pantalla de conciliación contable para certificar el sistema frente a las bitácoras físicas de minería:
$$P_{reg} = \left( \frac{\text{Registros Validados en Sistema}}{\text{Eventos Reales en Bitácora Física}} \right) \times 100$$

### 3.2 Resultado Operativo del Período (RF-26)
Calculado periódicamente por la Cloud Function `consolidarPeriodo` dentro de un rango de fechas:
$$\text{Resultado Operativo} = \sum \text{Ingresos (Despachos)} - \sum \text{Gastos Operativos}$$

---

## 4. Indicadores de Calidad de Ingeniería (ISO/IEC 25010)

- **Tasa de Entregabilidad de Sincronización (TES) (RNF-09):** Mide la confiabilidad del pipeline Offline-First:
  $$TES = \left( \frac{\text{Reportes Sincronizados en Nube}}{\text{Reportes Generados en Móvil}} \right) \times 100 \ge 99.0\%$$
- **Comportamiento Temporal de Persistencia Local (CT-1) (RNF-01):** Percentil 95 del guardado local en disco de la app móvil ≤ 2.0 s.
- **Comportamiento Temporal de Consolidación (CT-2) (RNF-02):** Procesamiento de hasta 5 000 documentos en Cloud Functions ≤ 60 s.
- **Tiempo de Consolidación ($T_{cons}$):** Horas transcurridas desde el cierre del período hasta la emisión del reporte.
- **Uso de Recursos en Borde (RNF-04):** Consumo de memoria RAM móvil ≤ 300 MB y drenaje de batería ≤ 15% en una jornada continua de 8 horas.

---

## 5. Reglas de Localización y Formatos de Datos (RNF-24)

- **Moneda:** Dólares estadounidenses (USD) con 2 decimales (`BigDecimal` en Kotlin / `number` con redondeo estricto a 2 decimales en TypeScript). Montos deben ser $> 0$.
- **Zona Horaria Oficial:** `America/Guayaquil` (UTC-05:00).
- **Formato de Fechas:** 
  - Almacenamiento / Persistencia: Timestamp de Firestore o string ISO 8601 (`YYYY-MM-DDTHH:mm:ssZ`).
  - Capa de Presentación (UI): `DD/MM/AAAA`.
- **Formato de Horas:** 24 horas (`HH:mm`).
- **Horómetros y Horas:** Números con 1 solo decimal.
- **Cantidades de Pétreos:** Metros cúbicos (`m3`) con 1 decimal o número entero de `viajes`.