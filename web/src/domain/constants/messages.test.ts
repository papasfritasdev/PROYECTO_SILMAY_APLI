import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { APP_MESSAGES, textoMensaje } from './messages';

/** Extrae `| **MSG-XX** | `texto` |` de la tabla oficial del catálogo. */
function leerCatalogoOficial(): Record<string, string> {
  const md = readFileSync(resolve(process.cwd(), '../docs/messages-catalog.md'), 'utf8');
  const filas = md.matchAll(/^\|\s*\*\*(MSG-\d{2})\*\*\s*\|\s*`([^`]+)`/gm);
  return Object.fromEntries([...filas].map(([, codigo, texto]) => [codigo, texto]));
}

describe('Catálogo de mensajes web', () => {
  it('coincide literalmente con docs/messages-catalog.md (sin faltantes ni sobrantes)', () => {
    const oficial = leerCatalogoOficial();
    expect(Object.keys(oficial).length).toBeGreaterThanOrEqual(18);
    expect(APP_MESSAGES).toEqual(oficial);
  });

  it('textoMensaje devuelve el texto del código', () => {
    expect(textoMensaje('MSG-13')).toBe('Esta plataforma no está habilitada para su rol.');
  });
});
