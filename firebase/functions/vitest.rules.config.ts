import { defineConfig } from 'vitest/config';

/**
 * Configuración de Vitest para la batería de Security Rules (HT-05).
 * Requiere Firestore Emulator: usar `npm run test:rules`.
 */
export default defineConfig({
  test: {
    include: ['test/rules/**/*.test.ts'],
    testTimeout: 20000,
    hookTimeout: 30000,
    fileParallelism: false,
    // Los PERMISSION_DENIED son el resultado esperado de las pruebas negativas.
    onConsoleLog: (log: string) => !log.includes('PERMISSION_DENIED')
  }
});
