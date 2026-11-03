# Guía y Plantilla de Cierre de Sprint (Handover) — SILMAY

Esta plantilla define la estructura técnica obligatoria que debe contener todo archivo `docs/sprints-handover/handover-sprint-[N].md` al finalizar un ciclo de desarrollo.

---

## Estructura Canónica Obligatoria

````markdown
# Informe de Relevo Técnico — Sprint [N]: [Título del Sprint]

- **Fecha de Cierre:** [DD/MM/AAAA]
- **Desarrollador Responsable:** [Nombre del Desarrollador]
- **Asistente IA Utilizado:** [Google Antigravity / Claude]
- **Puntos de Historia Completados:** [X] / [Total Pts Sprint]

---

## 1. Historias Implementadas y Estado
| ID | Tipo | Título | Pts | Estado | Enlace a PR |
| :--- | :--- | :--- | :--- | :--- | :--- |
| HT-XX | Técnica | ... | X | Completada | #PR_NUM |
| HU-XX | Usuario | ... | X | Completada | #PR_NUM |

---

## 2. Mapa de Archivos Clave Creados o Modificados
Detalle de archivos en el monorepo listos para consumo o extensión:
- **Capa Dominio (`domain/`):**
  - Entidades: `ruta/a/Entidad.kt`
  - Repositorios (Interfaces): `ruta/a/IRepository.kt`
  - Casos de Uso: `ruta/a/UseCase.kt`
- **Capa Datos (`data/`):**
  - Implementaciones: `ruta/a/RepositoryImpl.kt`
  - DTOs / Mappers: `ruta/a/Mapper.kt`
- **Capa Presentación (`presentation/` o `features/`):**
  - Vistas / Componentes: `ruta/a/Screen.kt` o `.tsx`
  - ViewModels / Hooks: `ruta/a/ViewModel.kt` o `useHook.ts`
- **Backend e Infraestructura (`firebase/`):**
  - Functions: `firebase/functions/src/...`
  - Rules / Indexes: `firestore.rules`, `firestore.indexes.json`

---

## 3. Contratos de Datos y Esquema Firestore
- Colecciones creadas o afectadas en este sprint.
- Identificadores de documentos de prueba generados en el emulador local para pruebas de integración.

---

## 4. Auditoría de Calidad y Definición de Terminado (DoD)
- [ ] Criterios Gherkin verificados y validados.
- [ ] Cobertura de pruebas unitarias en Dominio: **[XX.X]%** (≥ 70% exigido).
- [ ] Regla de dependencias de Clean Architecture validada (cero imports de SDKs en Dominio).
- [ ] Catálogo de mensajes verificado: todos los textos corresponden a `MSG-01` .. `MSG-18`.
- [ ] Cero credenciales privadas o secretos versionados en Git.
- [ ] Pipeline de CI en GitHub Actions en estado verde.

---

## 5. Puntos de Integración y Advertencias para el Siguiente Sprint
- **Funciones e interfaces listas para ser consumidas:** [Detalle]
- **Advertencias y "Gotchas":** [Ej. variables de entorno requeridas, dependencias de emulador, particularidades técnicas]
- **Objetivo de arranque sugerido para el Sprint [N+1]:** [Detalle]
````

---

## Protocolo de la Sesión de Relevo Humano (Walkthrough)

1. El desarrollador saliente realiza el merge formal a `develop` tras validar el handover con su IA.
2. Se agenda una sesión de 30 minutos entre ambos desarrolladores.
3. El desarrollador entrante:
   - Hace `git pull origin develop` en su entorno local.
   - Levanta los emuladores de Firebase (`firebase emulators:start`).
   - Ejecuta la suite de pruebas unitarias (`./gradlew test` o `npm test`) y verifica que pasen en verde en su propia máquina.
4. Ambos firman la conformidad del handover antes de iniciar el siguiente ciclo con su respectiva IA.