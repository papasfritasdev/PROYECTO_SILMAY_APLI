/**
 * Integración de FirebaseUsuariosRepository con Functions (gestionarUsuario) + Auth +
 * Firestore Emulator y Security Rules (HU-02). Ejecutar con: npm run test:integration
 */
import { signInWithEmailAndPassword } from 'firebase/auth';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { DatosUsuario, Usuario } from '../../domain/model/Usuario';
import {
  CONTRASENA_PRUEBA,
  crearCuenta,
  crearInstancia,
  limpiarEmuladores,
  type InstanciaFirebase
} from '../../test/emuladores';
import { FirebaseUsuariosRepository } from './FirebaseUsuariosRepository';

let firebase: InstanciaFirebase;
let repo: FirebaseUsuariosRepository;

async function iniciarComo(rol: 'admin' | 'contador'): Promise<void> {
  const { email } = await crearCuenta({ rol });
  await signInWithEmailAndPassword(firebase.auth, email, CONTRASENA_PRUEBA);
}

function alta(cambios: Partial<DatosUsuario> = {}): DatosUsuario {
  return {
    nombres: 'Juan Pérez',
    cedula: '0926687856',
    telefono: '0991234567',
    email: 'juan.perez@silmay.ec',
    password: CONTRASENA_PRUEBA,
    rol: 'operador',
    tipoLicencia: null,
    zonaAsignada: 'cantera-banos',
    cargo: 'Operador',
    estado: true,
    ...cambios
  };
}

function esperarUsuarios(condicion: (usuarios: readonly Usuario[]) => boolean): Promise<readonly Usuario[]> {
  return new Promise((resolver, rechazar) => {
    const cancelar = repo.observarUsuarios(
      (usuarios) => {
        if (condicion(usuarios)) {
          cancelar();
          resolver(usuarios);
        }
      },
      (mensaje) => rechazar(new Error(mensaje))
    );
  });
}

beforeEach(async () => {
  await limpiarEmuladores();
  firebase = crearInstancia();
  repo = new FirebaseUsuariosRepository(firebase.db, firebase.functions);
});

afterEach(async () => {
  await firebase.cerrar();
});

describe('FirebaseUsuariosRepository + gestionarUsuario (emuladores)', () => {
  it('CA1: el admin crea un usuario y aparece en el listado en tiempo real', async () => {
    await iniciarComo('admin');

    expect((await repo.guardarUsuario(alta())).ok).toBe(true);

    const usuarios = await esperarUsuarios((lista) => lista.some((u) => u.email === 'juan.perez@silmay.ec'));
    expect(usuarios.find((u) => u.email === 'juan.perez@silmay.ec')).toMatchObject({
      rol: 'operador',
      estado: true,
      telefono: '0991234567'
    });
  });

  it('CA2: un correo duplicado retorna MSG-17', async () => {
    await iniciarComo('admin');
    await repo.guardarUsuario(alta());

    expect(await repo.guardarUsuario(alta({ cedula: '0922879580' }))).toEqual({ ok: false, mensaje: 'MSG-17' });
  });

  it('CA3: la baja lógica se refleja en el listado', async () => {
    await iniciarComo('admin');
    const creado = await repo.guardarUsuario(alta());
    if (!creado.ok) throw new Error('alta fallida');

    const { password: _sinCambio, ...sinPassword } = alta();
    expect((await repo.guardarUsuario({ ...sinPassword, uid: creado.data, estado: false })).ok).toBe(true);

    const usuarios = await esperarUsuarios((lista) => lista.some((u) => u.uid === creado.data && !u.estado));
    expect(usuarios.find((u) => u.uid === creado.data)?.estado).toBe(false);
  });

  it('un contador no puede gestionar usuarios: MSG-13', async () => {
    await iniciarComo('contador');
    expect(await repo.guardarUsuario(alta())).toEqual({ ok: false, mensaje: 'MSG-13' });
  });

  it('las Security Rules impiden al contador listar usuarios: MSG-13', async () => {
    await iniciarComo('contador');
    await expect(esperarUsuarios(() => true)).rejects.toThrow('MSG-13');
  });
});
