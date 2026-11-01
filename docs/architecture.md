# Diseño Arquitectónico del Sistema SILMAY

- **Documento Fuente:** Diseño Arquitectónico v2.0 (ADR-0001 a ADR-0010)
- **Patrón de Diseño:** Clean Architecture + MVVM
- **Paradigma Núcleo:** Offline-First con sincronización diferida
- **Stack Principal:** Android (Kotlin + Jetpack Compose), React 18 (Vite + TypeScript), Firebase BaaS (Firestore, Functions, Auth, Rules)

---

## 1. Estilos Arquitectónicos del Sistema

El Sistema SILMAY combina tres estilos arquitectónicos complementarios:

1. **Offline-First en Cliente Móvil:** La desconexión es el estado ordinario de trabajo. Toda lectura y mutación se efectúa sobre la persistencia local en disco. La red actúa únicamente como un bus de transporte asíncrono.
2. **Clean Architecture y Patrón MVVM:** Desacoplamiento total entre las reglas de negocio y los frameworks. La capa de Presentación expone estados reactivos inmutables (`UiState` vía `StateFlow` en Android; custom hooks en React).
3. **Pipeline Borde-Nube Dirigido por Eventos:** Microservicios serverless en backend donde las mutaciones (`onDocumentWritten`) disparan validaciones, recálculo atómico de horómetros y emisión de alertas.

---

## 2. Frontera Arquitectónica: Módulos vs Componentes

Para mantener la separación de responsabilidades, el sistema distingue entre módulos funcionales de usuario y componentes de infraestructura:

| ID | Nombre | Tipo | Plataforma | Casos de Uso | Responsabilidad Principal |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **M1** | Captura de Jornada y Maquinaria | Módulo | Android | CU-03, CU-04 | Turnos, horómetros, deducciones y fallas con severidad. |
| **M2** | Venta y Despacho de Pétreos | Módulo | Android | CU-02 | Despachos offline, clientes, volquetas y pagos. |
| **M3** | Registro de Gastos Diarios | Módulo | Android | CU-05 | Egresos de campo, comprobantes y totalizador diario. |
| **M4** | Persistencia Local y Sync | Módulo | Móvil + Nube | CU-06 | Caché en disco, cola FIFO, reintentos y métrica TES. |
| **M5** | Monitoreo y Filtrado Operativo | Módulo | Web React | CU-08, CU-09 | Grillas reactivas, validación y conciliación contable. |
| **M6** | Reportería y Consolidación | Módulo | Web React | CU-10 | Resultado operativo, consolidación y exportación Excel/PDF. |
| **M7** | Autenticación y RBAC | Módulo | Móvil + Web | CU-01, CU-11 | Sesiones, Custom Claims y catálogo de cuentas. |
| **M8** | Gestión de Flota y Parámetros | Módulo | Web React | CU-12, CU-13 | Semáforo preventivo, mantenimientos y catálogos. |
| **C1** | Persistencia Centralizada | Componente | Nube (Firestore)| CU-06 | Base documental NoSQL multirregión (`southamerica-east1`). |
| **C2** | Lógica de Negocio Central | Componente | Nube (Functions)| CU-07, CU-10 | Recálculo de horómetros, alertas y agregaciones. |
| **C3** | Identidad y Seguridad | Componente | Nube (Auth/Rules)| CU-01 | Verificación de tokens y evaluación de Security Rules. |

---

## 3. Vista Lógica: Capas de Clean Architecture

Se aplica de forma incondicional la **Regla de Dependencias**: el código fuente solo apunta hacia adentro, hacia las entidades de negocio.

```text
┌────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                   │
│  Android: Jetpack Compose, ViewModels, StateFlow       │
│  Web: React 18, Componentes funcionales, Custom Hooks  │
└───────────────────────────┬────────────────────────────┘
                            │ depends on
┌───────────────────────────▼────────────────────────────┐
│                    DOMAIN LAYER (CORE)                 │
│  Kotlin / TypeScript Puro                              │
│  - Entidades de negocio (Jornada, Despacho, etc.)      │
│  - Value Objects (HorasEfectivas, Dinero)              │
│  - Interfaces de Repositorios                          │
│  - Casos de Uso (Use Cases)                            │
│  * ESTRICTAMENTE PROHIBIDO IMPORTAR FRAMEWORKS O SDKs *│
└───────────────────────────▲────────────────────────────┘
                            │ implements
┌───────────────────────────┴────────────────────────────┐
│                        DATA LAYER                      │
│  Implementación de contratos de repositorios           │
│  Data Transfer Objects (DTO) y Mappers bidireccionales │
│  Conexión a Firebase Auth, Firestore SDK y Disco       │
└────────────────────────────────────────────────────────┘
```

*(Estructura de capas de Clean Architecture para clientes SILMAY).*

---

## 4. Organización Física del Monorepo (ADR-0001)

El repositorio monorepo organiza el código en cuatro subproyectos desacoplados:

```text
SILMAY/
├── android/                         # Proyecto Android nativo (ec.silmay.app)
│   └── app/src/main/java/ec/silmay/app/
│       ├── domain/                  # model/ · repository/ (interfaces) · usecase/
│       ├── data/                    # firestore/ · auth/ · mapper/ · repository/ (impl)
│       ├── presentation/            # login/ · jornada/ · despacho/ · gasto/ · registros/
│       └── di/                      # Módulos Hilt para inyección de dependencias
├── web/                             # Plataforma Web React 18 + Vite + TS
│   └── src/
│       ├── domain/                  # Entidades, usecases y contratos TypeScript puros
│       ├── data/                    # Firebase Web SDK, mappers y repositorios
│       ├── hooks/                   # Custom hooks reactivos (useAuth, useFirestoreQuery)
│       └── features/                # monitoreo/ · validacion/ · reporteria/ · alertas/
│                                    # usuarios/ · maquinaria/ · catalogos/
├── firebase/                        # Backend Serverless e Infraestructura como Código
│   ├── firestore.rules              # Reglas declarativas de seguridad autoritativas
│   ├── firestore.indexes.json       # Definición de índices compuestos
│   └── functions/src/               # Microservicios en TypeScript (Node.js runtime)
├── scripts/                         # Scripts locales: alta de admin y carga de catálogos
└── docs/                            # Documentación técnica, contratos y manuales
```

---

## 5. Registro de Decisiones de Arquitectura (ADRs)

- **ADR-0001 (Monorepo):** Repositorio único en Git para asegurar trazabilidad unificada y contratos compartidos en cada sprint.
- **ADR-0002 (Stack Tecnológico):** Kotlin nativo + Jetpack Compose para móvil; React 18 + TypeScript para web; Cloud Functions en Node.js.
- **ADR-0003 (Gestión de Identidad y RBAC):** Firebase Auth con Custom Claims inyectados autoritativamente desde el servidor.
- **ADR-0004 (Estrategia Offline-First):** Persistencia nativa en disco del SDK de Firestore como mecanismo primario validado en HT-06 (alternativa de contingencia: Room + WorkManager).
- **ADR-0005 (Idempotencia en Sincronización):** Generación de UUID v4 en el cliente como Document ID y mutación obligatoria mediante `set()` para evitar duplicados.
- **ADR-0006 (Cuatro Roles de Usuario):** Segregación formal de perfiles: Operador, Supervisor, Contador y Administrador.
- **ADR-0007 (Deducciones Embebidas):** La jornada es un documento único con arreglo de mapas embebido para tiempos improductivos, eliminando subcolecciones.
- **ADR-0008 (Novedades en Jornada):** Las fallas mecánicas se registran como deducciones con severidad; las críticas disparan alertas automáticas.
- **ADR-0009 (Región de Firestore):** Despliegue estricto e idéntico en `southamerica-east1` para entornos dev y prod.
- **ADR-0010 (Campo 'zona' para Supervisión):** Desnormalización de la zona asignada del usuario en documentos operativos para filtrado directo de supervisores.