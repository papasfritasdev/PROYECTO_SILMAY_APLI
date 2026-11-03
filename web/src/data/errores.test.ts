import { FirebaseError } from 'firebase/app';
import { describe, expect, it } from 'vitest';
import { esPermisoDenegado, mapearErrorServidor } from './errores';
import { mapearUsuario } from './mapper/usuarioMapper';

describe('mapearErrorServidor (docs/cloud-functions.md §2.2)', () => {
  it('already-exists se traduce al mensaje de duplicado del contexto', () => {
    const error = new FirebaseError('functions/already-exists', 'dup');
    expect(mapearErrorServidor(error, 'MSG-17')).toBe('MSG-17');
    expect(mapearErrorServidor(error, 'MSG-09')).toBe('MSG-09');
  });

  it('permisos y autenticación → MSG-13 (con o sin prefijo functions/)', () => {
    expect(mapearErrorServidor(new FirebaseError('functions/permission-denied', 'x'))).toBe('MSG-13');
    expect(mapearErrorServidor(new FirebaseError('permission-denied', 'x'))).toBe('MSG-13');
    expect(mapearErrorServidor(new FirebaseError('functions/unauthenticated', 'x'))).toBe('MSG-13');
  });

  it('errores inesperados o de red → MSG-18', () => {
    expect(mapearErrorServidor(new FirebaseError('functions/internal', 'x'))).toBe('MSG-18');
    expect(mapearErrorServidor(new FirebaseError('unavailable', 'x'))).toBe('MSG-18');
    expect(mapearErrorServidor(new Error('boom'))).toBe('MSG-18');
  });

  it('esPermisoDenegado identifica rechazos de Security Rules', () => {
    expect(esPermisoDenegado(new FirebaseError('permission-denied', 'x'))).toBe(true);
    expect(esPermisoDenegado(new FirebaseError('not-found', 'x'))).toBe(false);
    expect(esPermisoDenegado(new Error('x'))).toBe(false);
  });
});

describe('mapearUsuario', () => {
  it('mapea el perfil completo y tolera campos opcionales', () => {
    expect(
      mapearUsuario('u1', { email: 'a@silmay.ec', rol: 'contador', nombres: 'Ana', estado: true })
    ).toEqual({
      uid: 'u1',
      nombres: 'Ana',
      cedula: '',
      telefono: '',
      email: 'a@silmay.ec',
      rol: 'contador',
      tipoLicencia: null,
      zonaAsignada: '',
      cargo: '',
      estado: true
    });
  });

  it('descarta documentos sin rol válido o sin correo', () => {
    expect(mapearUsuario('u1', { email: 'a@silmay.ec', rol: 'jefe' })).toBeNull();
    expect(mapearUsuario('u1', { rol: 'admin' })).toBeNull();
  });
});
