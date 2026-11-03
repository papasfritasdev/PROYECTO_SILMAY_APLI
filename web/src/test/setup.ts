import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Vitest se usa sin globals: la limpieza automática de Testing Library se registra aquí.
afterEach(() => {
  cleanup();
});
