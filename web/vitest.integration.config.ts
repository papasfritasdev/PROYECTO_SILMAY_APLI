import { defineConfig } from 'vitest/config';

/**
 * Pruebas de integración web contra Auth + Firestore Emulator (con Security Rules).
 * Usar `npm run test:integration`.
 */
export default defineConfig({
  test: {
    environment: 'node',
    include: ['src/**/*.integration.test.ts'],
    testTimeout: 20000,
    hookTimeout: 30000,
    fileParallelism: false
  }
});
