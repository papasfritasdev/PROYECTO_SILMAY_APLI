import { describe, expect, it } from 'vitest';
import type { DatosUsuario } from '../model/Usuario';
import { normalizarUsuario, validarUsuario } from './validarUsuario';

const valido: DatosUsuario = {
  nombres: 'Juan Pérez',
  cedula: '1710034065',
  telefono: '0991234567',
  email: 'juan@silmay.ec',
  password: 'Silmay2026',
  rol: 'operador',
  tipoLicencia: null,
  zonaAsignada: 'cantera-banos',
  cargo: 'Operador',
  estado: true
};

describe('validarUsuario (HU-02 CA1)', () => {
  it('un alta completa no tiene campos inválidos', () => {
    expect(validarUsuario(valido)).toEqual([]);
  });

  it('detecta cada campo obligatorio o mal formado', () => {
    const invalido: DatosUsuario = {
      ...valido,
      nombres: '  ',
      cedula: '123',
      telefono: '555',
      email: 'x',
      password: 'corta',
      zonaAsignada: '',
      cargo: ''
    };
    expect(validarUsuario(invalido).sort()).toEqual(
      ['cargo', 'cedula', 'email', 'nombres', 'password', 'telefono', 'zonaAsignada'].sort()
    );
  });

  it('rechaza un rol fuera del catálogo', () => {
    expect(validarUsuario({ ...valido, rol: 'gerente' as DatosUsuario['rol'] })).toEqual(['rol']);
  });

  it('la contraseña es obligatoria al crear', () => {
    expect(validarUsuario({ ...valido, password: '' })).toEqual(['password']);
  });

  it('al editar, la contraseña vacía es válida, pero si se informa debe cumplir la política', () => {
    expect(validarUsuario({ ...valido, uid: 'u1', password: '' })).toEqual([]);
    expect(validarUsuario({ ...valido, uid: 'u1', password: undefined })).toEqual([]);
    expect(validarUsuario({ ...valido, uid: 'u1', password: 'abc' })).toEqual(['password']);
  });
});

describe('normalizarUsuario', () => {
  it('recorta, pasa el correo a minúsculas y limpia el teléfono', () => {
    const normalizado = normalizarUsuario({
      ...valido,
      nombres: '  Juan Pérez ',
      email: ' Juan@SILMAY.ec ',
      telefono: '099 123-4567',
      tipoLicencia: '  ',
      zonaAsignada: ' cantera-banos ',
      cargo: ' Operador '
    });
    expect(normalizado).toMatchObject({
      nombres: 'Juan Pérez',
      email: 'juan@silmay.ec',
      telefono: '0991234567',
      tipoLicencia: null,
      zonaAsignada: 'cantera-banos',
      cargo: 'Operador'
    });
  });

  it('omite la contraseña vacía para no cambiarla al editar', () => {
    expect(normalizarUsuario({ ...valido, uid: 'u1', password: '' }).password).toBeUndefined();
    expect(normalizarUsuario({ ...valido, tipoLicencia: ' Tipo G ' }).tipoLicencia).toBe('Tipo G');
  });
});
