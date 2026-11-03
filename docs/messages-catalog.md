# Catálogo Oficial de Mensajes del Sistema SILMAY (UI y Validaciones)

Este catálogo define los textos literales que deben desplegarse en pantalla ante eventos de validación, errores y control de flujo.

> **Regla de Desarrollo:** Queda estrictamente prohibido que la aplicación móvil o la plataforma web muestren mensajes hardcodeados o inventados. Todo mensaje debe corresponder a esta tabla canónica.

---

| Código | Texto Exacto en Pantalla | Disparador / Origen Técnico | Módulos Asociados |
| :--- | :--- | :--- | :--- |
| **MSG-01** | `Credenciales no válidas. Revise su usuario o contraseña.` | Error en inicio de sesión (mensaje opaco sin revelar campo erróneo). | M7 (RF-01 / CU-01) |
| **MSG-02** | `Se requiere conexión a Internet para el registro inicial de su cuenta en este dispositivo.` | Primer login sin red en la aplicación móvil. | M7 (RF-04 / CU-01) |
| **MSG-03** | `Cuenta deshabilitada. Contacte con la administración de SILMAY.` | Intento de autenticación con una cuenta marcada como `estado: false`. | M7 (RF-03 / CU-01) |
| **MSG-04** | `Memoria insuficiente en el dispositivo para guardar el registro.` | Espacio de almacenamiento local lleno en el dispositivo móvil. | M2 (RF-13 / CU-02) |
| **MSG-05** | `El horómetro final no puede ser menor al horómetro inicial. Por favor verifique el tablero de la máquina.` | Inconsistencia en horómetro final durante el cierre de jornada (`horometroFinal <= horometroInicial`). | M1 (RF-09 / CU-03) |
| **MSG-06** | `La descripción de la novedad debe tener al menos 10 caracteres.` | Descripción de falla técnica o avería con longitud insuficiente (`length < 10`). | M1 (RF-08 / CU-04) |
| **MSG-07** | `Ingrese un valor monetario válido.` | Monto inválido o menor/igual a cero en registro de egreso operativo (`montoUSD <= 0`). | M3 (RF-14 / CU-05) |
| **MSG-08** | `No se encontraron despachos ni movimientos operativos registrados en el período seleccionado.` | Generación de consolidación mensual sin registros coincidentes en las fechas. | M6 (RF-25 / CU-10) |
| **MSG-09** | `Ya existe una unidad de maquinaria registrada con ese código/placa.` | Duplicidad en `codigoInterno` o `placa` al registrar maquinaria o volqueta. | M8 (RF-30 / CU-12) |
| **MSG-10** | `No existen movimientos que coincidan con los filtros seleccionados.` | Búsqueda o filtrado en grillas web de monitoreo sin resultados coincidentes. | M5 (RF-22 / CU-08) |
| **MSG-11** | `La hora de salida debe ser posterior a la hora de inicio de la jornada.` | Cierre de jornada con `horaSalida <= horaInicio`. | M1 (RF-09 / CU-03) |
| **MSG-12** | `Las horas restadas no pueden superar la duración de la jornada.` | Sumatoria de deducciones (`sum(horasRestadas) >= horasBrutas`). | M1 (RF-09 / CU-03) |
| **MSG-13** | `Esta plataforma no está habilitada para su rol.` | Intento de login de Operador/Supervisor en Web, o de Contador/Admin en Móvil. | M7 (RF-02 / CU-01) |
| **MSG-14** | `Tiene una jornada abierta. Finalícela antes de iniciar otra.` | Intento de abrir una nueva jornada cuando el operador ya tiene una en estado `abierta`. | M1 (RF-06 / CU-03) |
| **MSG-15** | `Registre un comentario para marcar el registro como Observado.` | Intento de guardar una observación financiera en contabilidad sin redactar justificación. | M5 (RF-24 / CU-09) |
| **MSG-16** | `Ya existe un registro con ese valor en el catálogo.` | Duplicidad de nombre al agregar un nuevo ítem en `catalogos/{tipo}/items`. | M8 (RF-32 / CU-13) |
| **MSG-17** | `Ya existe un usuario registrado con ese correo o cédula.` | Duplicidad de `email` o `cedula` al crear o actualizar una cuenta en `usuarios` (`gestionarUsuario` responde `already-exists`). Agregado en Sprint 2 por acuerdo del equipo (HU-02 CA2). | M7 (RF-05 / CU-11) |

---

## Implementación Técnica Recomendada

- **En Android (Kotlin):** Representar estos mensajes como un `enum class AppMessage(val code: String, val messageRes: Int)` referenciando `res/values/strings.xml` para soporte de UI en Jetpack Compose.
- **En Web (TypeScript):** Representar como un objeto inmutable de constantes tipadas `APP_MESSAGES[code]` dentro de `src/domain/constants/messages.ts`.