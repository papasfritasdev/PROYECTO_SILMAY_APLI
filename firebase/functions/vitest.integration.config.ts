import { defineConfig } from 'vitest/config';

/**
 * Pruebas de integración de Cloud Functions contra Auth + Firestore Emulator.
 * Usar `npm run test:integration`.
 */
export default defineConfig({
  test: {
    include: ['test/integration/**/*.test.ts'],
    testTimeout: 20000,
    hookTimeout: 30000,
    fileParallelism: false
  }
});
