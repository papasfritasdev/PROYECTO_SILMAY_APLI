import { describe, expect, it } from 'vitest';
import { esRol, habilitadoEnWeb, ROLES } from './Rol';

describe('Rol', () => {
  it('reconoce exactamente los 4 roles canónicos', () => {
    expect(ROLES).toEqual(['operador', 'supervisor', 'contador', 'admin']);
    ROLES.forEach((rol) => expect(esRol(rol)).toBe(true));
    expect(esRol('ADMIN')).toBe(false);
    expect(esRol('gerente')).toBe(false);
    expect(esRol(undefined)).toBe(false);
    expect(esRol(1)).toBe(false);
  });

  it('HU-01b: solo contador y admin están habilitados en web', () => {
    expect(habilitadoEnWeb('contador')).toBe(true);
    expect(habilitadoEnWeb('admin')).toBe(true);
    expect(habilitadoEnWeb('operador')).toBe(false);
    expect(habilitadoEnWeb('supervisor')).toBe(false);
  });
});
