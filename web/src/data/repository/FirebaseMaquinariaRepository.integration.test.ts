/**
 * Integración de FirebaseMaquinariaRepository con Firestore + Security Rules reales y la
 * callable registrarRechazo (HU-09). Ejecutar con: npm run test:integration
 */
import { signInWithEmailAndPassword } from 'firebase/auth';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { Maquina } from '../../domain/model/Maquina';
import {
  CONTRASENA_PRUEBA,
  crearCuenta,
  crearInstancia,
  leerColeccionComoOwner,
  limpiarEmuladores,
  type InstanciaFirebase
} from '../../test/emuladores';
import { FirebaseMaquinariaRepository } from './FirebaseMaquinariaRepository';
import { RegistradorRechazosFirebase } from './RegistradorRechazos';

let firebase: InstanciaFirebase;
let repo: FirebaseMaquinariaRepository;

const volqueta: Maquina = {
  codigoInterno: 'VOL-03',
  placa: 'TBA-1234',
  marca: 'Hino',
  modelo: '500',
  tipoEquipo: 'volqueta',
  horometroBase: 120.5,
  horometroActual: 120.5,
  intervaloMantenimiento: 500,
  proximoMantenimientoHoras: 620.5,
  estado: 'operativa'
};

async function iniciarComo(rol: 'admin' | 'contador'): Promise<string> {
  const { uid, email } = await crearCuenta({ rol });
  await signInWithEmailAndPassword(firebase.auth, email, CONTRASENA_PRUEBA);
  return uid;
}

function esperarFlota(condicion: (flota: readonly Maquina[]) => boolean): Promise<readonly Maquina[]> {
  return new Promise((resolver, rechazar) => {
    const cancelar = repo.observarMaquinaria(
      (flota) => {
        if (condicion(flota)) {
          cancelar();
          resolver(flota);
        }
      },
      (mensaje) => rechazar(new Error(mensaje))
    );
  });
}

beforeEach(async () => {
  await limpiarEmuladores();
  firebase = crearInstancia();
  repo = new FirebaseMaquinariaRepository(firebase.db, new RegistradorRechazosFirebase(firebase.functions));
});

afterEach(async () => {
  await firebase.cerrar();
});

describe('FirebaseMaquinariaRepository + Security Rules (emuladores)', () => {
  it('CA1: el admin registra una volqueta que las reglas aceptan y aparece en la flota', async () => {
    await iniciarComo('admin');

    expect(await repo.registrarMaquina(volqueta)).toEqual({ ok: true, data: undefined });

    const flota = await esperarFlota((lista) => lista.some((m) => m.codigoInterno === 'VOL-03'));
    expect(flota.find((m) => m.codigoInterno === 'VOL-03')).toEqual(volqueta);
  });

  it('CA2: un código existente retorna MSG-09 y NO sobrescribe la unidad original', async () => {
    await iniciarComo('admin');
    await repo.registrarMaquina(volqueta);

    const resultado = await repo.registrarMaquina({ ...volqueta, placa: 'PBC-0001', marca: 'Otra' });

    expect(resultado).toEqual({ ok: false, mensaje: 'MSG-09' });
    const flota = await esperarFlota((lista) => lista.length === 1);
    expect(flota[0].marca).toBe('Hino');
  });

  it('CA2: una placa ya registrada en otra unidad retorna MSG-09', async () => {
    await iniciarComo('admin');
    await repo.registrarMaquina(volqueta);

    expect(await repo.registrarMaquina({ ...volqueta, codigoInterno: 'VOL-04' })).toEqual({
      ok: false,
      mensaje: 'MSG-09'
    });
    expect(
      await repo.actualizarMaquina('VOL-99', { placa: 'TBA-1234', marca: 'X', modelo: 'Y', estado: 'operativa' })
    ).toEqual({ ok: false, mensaje: 'MSG-09' });
  });

  it('la baja lógica cambia el estado a inactiva (sin borrar el documento)', async () => {
    await iniciarComo('admin');
    await repo.registrarMaquina(volqueta);

    await repo.actualizarMaquina('VOL-03', { placa: 'TBA-1234', marca: 'Hino', modelo: '500', estado: 'inactiva' });

    const flota = await esperarFlota((lista) => lista[0]?.estado === 'inactiva');
    expect(flota).toHaveLength(1);
  });

  it('CA3: un contador es rechazado (MSG-13) y el rechazo queda en auditoria', async () => {
    const contadorUid = await iniciarComo('contador');

    expect(await repo.registrarMaquina(volqueta)).toEqual({ ok: false, mensaje: 'MSG-13' });

    const auditoria = await leerColeccionComoOwner('auditoria');
    expect(auditoria).toEqual([
      expect.objectContaining({
        accion: 'PERMISSION_DENIED',
        coleccion: 'maquinaria',
        documentoId: 'VOL-03',
        usuarioId: contadorUid
      })
    ]);
  });
});
