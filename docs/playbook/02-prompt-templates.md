# Plantillas de Prompts Estandarizadas — SILMAY

Utiliza estas plantillas para comunicarte con Google Antigravity o Claude. No modifiques la estructura de las instrucciones entre corchetes para evitar desvíos arquitectónicos.

---

## 1. Prompt para Iniciar una Tarea o Historia (Feature Prompt)

> **Uso:** Copiar al iniciar cualquier Historia de Usuario (HU) o Historia Técnica (HT).

```text
Actúa como Ingeniero de Software Principal en el proyecto SILMAY.
Vamos a implementar la historia: [ID_HISTORIA, ej: HU-01a] - [TÍTULO_HISTORIA].

Antes de escribir código:
1. Lee `docs/INDEX.md` y consulta específicamente `docs/specs/sprint-[N]-specs.md` para extraer los Criterios de Aceptación Gherkin de esta historia.
2. Consulta `docs/firebase-schema.md` para respetar los nombres exactos de colecciones y campos.
3. Consulta `docs/messages-catalog.md` para utilizar los códigos y textos literales oficiales (MSG-XX) en cualquier mensaje de validación o alerta.
4. Revisa `docs/playbook/04-ai-guardrails.md` para cumplir estrictamente con Clean Architecture y la Definición de Terminado (DoD).

Restricciones no negociables:
- La capa `domain` debe ser código puro sin frameworks ni SDKs.
- Si realizas mutaciones en Firestore, usa `.document(uuidV4).set(...)`, jamás `collection.add()`.
- Genera código modular, tipado estricto y sin tipos dinámicos (`any`).

Explícame brevemente el diseño técnico antes de escribir los archivos.
```

---

## 2. Prompt para Generación de Pruebas Unitarias (Testing Prompt)

> **Uso:** Copiar tras implementar la lógica de negocio para alcanzar el ≥ 70% de cobertura.

```text
Genera la suite de pruebas unitarias para el componente: [RUTA_DEL_ARCHIVO_O_CLASE, ej: IniciarSesionUseCase.kt].

Requerimientos de prueba:
1. Utiliza [MockK y CoroutineTestRule en Android / Vitest en Web].
2. Valida cada uno de los caminos de los Criterios de Aceptación Gherkin de la historia correspondiente.
3. Incluye pruebas para caminos felices, casos de borde y errores (ej. cuentas inactivas, roles incorrectos, credenciales inválidas).
4. Asegura que la cobertura de líneas y ramas de la capa de Dominio sea igual o superior al 70%.
5. No utilices mocks innecesarios en entidades de valor puro.
```

---

## 3. Prompt para Cierre de Sprint y Generación de Handover

> **Uso:** Copiar al terminar el sprint para que la IA genere el informe técnico de relevo en `docs/sprints-handover/`.

```text
El Sprint [N] ha concluido. Analiza todo el trabajo realizado en el repositorio durante este sprint.
Genera el informe técnico de relevo en `docs/sprints-handover/handover-sprint-0[N].md` siguiendo al pie de la letra la estructura definida en `docs/playbook/03-handover-guide.md`.

Asegúrate de:
1. Citar las rutas exactas de los archivos creados o modificados en `domain/`, `data/`, `presentation/`, `firebase/` y `scripts/`.
2. Detallar los modelos y contratos de base de datos creados.
3. Reportar el porcentaje de cobertura de pruebas unitarias alcanzado.
4. Incluir advertencias técnicas, dependencias pendientes y puntos clave de integración que el siguiente desarrollador debe conocer para arrancar el Sprint [N+1].
```