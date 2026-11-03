import { FirebaseError } from 'firebase/app';
import { describe, expect, it } from 'vitest';
import { mapearErrorAuth } from '../repository/FirebaseAuthRepository';
import { mapearUsuarioSesion } from './usuarioMapper';

const perfil = {
  uid: 'u1',
  nombres: 'Ana Contadora',
  email: 'ana@silmay.ec',
  rol: 'contador',
  zonaAsignada: 'administracion',
  estado: true
};

describe('mapearUsuarioSesion', () => {
  it('mapea un perfil completo', () => {
    expect(mapearUsuarioSesion('u1', perfil, 'contador')).toEqual({
      uid: 'u1',
      email: 'ana@silmay.ec',
      nombres: 'Ana Contadora',
      rol: 'contador',
      zonaAsignada: 'administracion',
      estado: true
    });
  });

  it('el Custom Claim prevalece sobre el rol del documento (ADR-0003)', () => {
    expect(mapearUsuarioSesion('u1', { ...perfil, rol: 'admin' }, 'operador')?.rol).toBe('operador');
  });

  it('sin claim válido usa el rol del documento', () => {
    expect(mapearUsuarioSesion('u1', perfil, undefined)?.rol).toBe('contador');
  });

  it('estado distinto de true se interpreta como baja lógica', () => {
    expect(mapearUsuarioSesion('u1', { ...perfil, estado: 'si' }, 'contador')?.estado).toBe(false);
  });

  it('retorna null ante perfiles inexistentes, incompletos o con rol desconocido', () => {
    expect(mapearUsuarioSesion('u1', undefined, 'admin')).toBeNull();
    expect(mapearUsuarioSesion('u1', { ...perfil, email: undefined }, 'contador')).toBeNull();
    expect(mapearUsuarioSesion('u1', { ...perfil, rol: 'gerente' }, 'gerente')).toBeNull();
  });
});

describe('mapearErrorAuth', () => {
  it('cuenta deshabilitada en Auth → MSG-03', () => {
    expect(mapearErrorAuth(new FirebaseError('auth/user-disabled', 'x'))).toBe('MSG-03');
  });

  it('fallo de red → MSG-18', () => {
    expect(mapearErrorAuth(new FirebaseError('auth/network-request-failed', 'x'))).toBe('MSG-18');
  });

  it('credenciales inválidas, usuario inexistente o error desconocido → MSG-01 opaco', () => {
    expect(mapearErrorAuth(new FirebaseError('auth/invalid-credential', 'x'))).toBe('MSG-01');
    expect(mapearErrorAuth(new FirebaseError('auth/user-not-found', 'x'))).toBe('MSG-01');
    expect(mapearErrorAuth(new Error('boom'))).toBe('MSG-01');
  });
});
