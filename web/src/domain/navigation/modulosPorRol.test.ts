import { describe, expect, it } from 'vitest';
import { modulosPorRol, puedeAccederModulo } from './modulosPorRol';

describe('Matriz RBAC web (HU-01b CA1)', () => {
  it('admin gestiona usuarios, maquinaria y catálogos pero no valida contabilidad', () => {
    const modulos = modulosPorRol('admin');
    expect(modulos).toEqual(
      expect.arrayContaining(['usuarios', 'maquinaria', 'catalogos', 'alertas', 'reporteria', 'monitoreo'])
    );
    expect(modulos).not.toContain('validacion');
  });

  it('contador valida y consulta, sin acceso a administración de usuarios ni flota', () => {
    const modulos = modulosPorRol('contador');
    expect(modulos).toEqual(expect.arrayContaining(['validacion', 'monitoreo', 'alertas', 'reporteria']));
    expect(modulos).not.toContain('usuarios');
    expect(modulos).not.toContain('maquinaria');
    expect(modulos).not.toContain('catalogos');
  });

  it('roles de campo no tienen módulos web', () => {
    expect(modulosPorRol('operador')).toEqual([]);
    expect(modulosPorRol('supervisor')).toEqual([]);
  });

  it('puedeAccederModulo aplica la matriz', () => {
    expect(puedeAccederModulo('admin', 'usuarios')).toBe(true);
    expect(puedeAccederModulo('contador', 'usuarios')).toBe(false);
    expect(puedeAccederModulo('operador', 'monitoreo')).toBe(false);
  });
});
