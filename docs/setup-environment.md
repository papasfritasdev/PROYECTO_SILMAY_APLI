# Configuración de Entornos y Emuladores — Sistema SILMAY

Este documento establece los parámetros de configuración para aislar los entornos de desarrollo (`silmay-dev`) y producción (`silmay-prod`), junto con la ejecución del Firebase Emulator Suite para pruebas locales.

---

## 1. Aislamiento de Proyectos en Firebase Console (HT-02)

El sistema opera bajo dos proyectos independientes en Google Cloud / Firebase (plan Blaze):

| Parámetro | Entorno de Desarrollo (`dev`) | Entorno de Producción (`prod`) |
| :--- | :--- | :--- |
| **Project ID** | `silmay-dev` | `silmay-prod` |
| **Región Firestore** | `southamerica-east1` (São Paulo) | `southamerica-east1` (São Paulo) |
| **Región Functions** | `southamerica-east1` | `southamerica-east1` |
| **Proveedor Auth** | Email / Contraseña | Email / Contraseña |
| **Uso Principal** | Pruebas locales, emuladores y CI/CD | Despliegue piloto formal en SILMAY |
| **Alertas Presupuesto** | 50%, 90% y 100% de USD 20,00/mes | 50%, 90% y 100% de USD 20,00/mes |

---

## 2. Configuración del Firebase Emulator Suite (Local)

Para desarrollar y ejecutar pruebas unitarias/integradas sin consumir cuota de red ni costos de Firebase, se utiliza el emulador local.

### 2.1 Puertos Estándar de Emuladores (`firebase.json`)
- **Emulator UI (Panel Gráfico):** `localhost:4000`
- **Authentication Emulator:** `localhost:9099`
- **Cloud Firestore Emulator:** `localhost:8080`
- **Cloud Functions Emulator:** `localhost:5001`

### 2.2 Comando de Inicio Local
Desde la carpeta raíz o dentro de `/firebase`:

```bash
# Iniciar emuladores con persistencia temporal de datos de prueba
firebase emulators:start --only auth,firestore,functions
```

---

## 3. Configuración por Plataforma

### 3.1 Android (`android/`)
- **Archivo de Credenciales:** `android/app/google-services.json`. En ramas de desarrollo (`develop`, `feature/*`), se utiliza el archivo correspondiente a `silmay-dev`.
- **Restricción DoD:** Prohibido comitear el archivo de producción con credenciales privadas en repositorios públicos.
- **Conexión a Emuladores en Modo Debug:**
  El código de inicialización de Firebase en Android debe apuntar a `10.0.2.2` (loopback del Android Emulator) cuando la app corre en modo `DEBUG`:

```kotlin
// Inyección condicional en BuildConfig.DEBUG
auth.useEmulator("10.0.2.2", 9099)
firestore.useEmulator("10.0.2.2", 8080)
```

### 3.2 Backend Serverless (`firebase/functions/`)
- **Archivo de Entorno Local:** `firebase/functions/.env.local`
- **Dependencias y Runtime:** Node.js 18 / 20 LTS, TypeScript 5.x.

### 3.3 Web (`web/`)
Archivos `.env.development` y `.env.production` en la raíz de `web/`.

Variables mínimas requeridas:

```env
VITE_FIREBASE_API_KEY=tu_api_key
VITE_FIREBASE_AUTH_DOMAIN=silmay-dev.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=silmay-dev
VITE_FIREBASE_STORAGE_BUCKET=silmay-dev.appspot.com
VITE_FIREBASE_MESSAGING_SENDER_ID=tu_sender_id
VITE_FIREBASE_APP_ID=tu_app_id
VITE_USE_FIREBASE_EMULATORS=true
```

**Modo emulador (Sprint 2, decisión del equipo):** sin archivos `.env`, la web usa valores de demostración
(`projectId: silmay-dev`) y se conecta automáticamente a los emuladores en `npm run dev`
(Auth `127.0.0.1:9099`, Firestore `127.0.0.1:8080`, Functions `127.0.0.1:5001`).
No versionar ningún archivo `.env*`: el CI rechaza cualquier ruta que contenga `.env`.

```bash
cd web
npm install
npm run dev               # http://localhost:5173 (requiere emuladores levantados)
npm test                  # pruebas unitarias y de componentes
npm run coverage          # cobertura del dominio (umbral 70%)
npm run test:integration  # repositorio real + Security Rules en emuladores (puertos libres)
```

---

## 4. Política de Secretos y Archivos Ignorados (`.gitignore`)

Los siguientes patrones deben estar obligatoriamente en el archivo `.gitignore` raíz:

```gitignore
*.env
*.env.local
*.env.production
google-services.json
firebase-adminsdk-*.json
service-account-key.json
node_modules/
dist/
build/
.gradle/
```

*(Regla de seguridad DoD: Cero claves privadas en el control de versiones).*