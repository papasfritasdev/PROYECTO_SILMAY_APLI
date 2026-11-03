import { describe, expect, it } from 'vitest';
import { esEstadoMaquina, esTipoEquipo, ESTADOS_MAQUINA, TIPOS_EQUIPO } from './Maquina';

describe('Maquina (firebase-schema §2.5)', () => {
  it('reconoce exactamente los tipos de equipo canónicos', () => {
    TIPOS_EQUIPO.forEach((tipo) => expect(esTipoEquipo(tipo)).toBe(true));
    expect(esTipoEquipo('grúa')).toBe(false);
    expect(esTipoEquipo(undefined)).toBe(false);
  });

  it('reconoce exactamente los estados canónicos', () => {
    expect(ESTADOS_MAQUINA).toEqual(['operativa', 'requiere_mantenimiento', 'inactiva']);
    ESTADOS_MAQUINA.forEach((estado) => expect(esEstadoMaquina(estado)).toBe(true));
    expect(esEstadoMaquina('averiada')).toBe(false);
    expect(esEstadoMaquina(3)).toBe(false);
  });
});
