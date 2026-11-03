import {
  collection,
  doc,
  getDocs,
  limit,
  onSnapshot,
  orderBy,
  query,
  runTransaction,
  updateDoc,
  where,
  type Firestore
} from 'firebase/firestore';
import type { CodigoMensaje } from '../../domain/constants/messages';
import type { CambiosMaquina, Maquina } from '../../domain/model/Maquina';
import { exito, fallo, type Resultado } from '../../domain/model/Resultado';
import type { MaquinariaRepository } from '../../domain/repository/MaquinariaRepository';
import { esPermisoDenegado, mapearErrorServidor } from '../errores';
import { mapearMaquina } from '../mapper/maquinaMapper';
import type { OperacionRechazada, RegistradorRechazos } from './RegistradorRechazos';

const COLECCION = 'maquinaria';

/** Señal interna de la transacción: el Document ID (codigoInterno) ya existe. */
class CodigoDuplicado extends Error {}

/**
 * Flota sobre Firestore (HU-09). Escrituras protegidas por las Security Rules de HT-05
 * (solo admin); los rechazos se auditan con `registrarRechazo` (CA3).
 */
export class FirebaseMaquinariaRepository implements MaquinariaRepository {
  constructor(
    private readonly db: Firestore,
    private readonly rechazos: RegistradorRechazos
  ) {}

  observarMaquinaria(
    alRecibir: (maquinas: readonly Maquina[]) => void,
    alFallar: (mensaje: CodigoMensaje) => void
  ): () => void {
    return onSnapshot(
      query(collection(this.db, COLECCION), orderBy('codigoInterno')),
      (snapshot) =>
        alRecibir(
          snapshot.docs
            .map((d) => mapearMaquina(d.id, d.data()))
            .filter((m): m is Maquina => m !== null)
        ),
      (error) => alFallar(mapearErrorServidor(error))
    );
  }

  async registrarMaquina(maquina: Maquina): Promise<Resultado<void>> {
    try {
      if (maquina.placa && (await this.placaEnUso(maquina.placa, maquina.codigoInterno))) {
        return fallo('MSG-09');
      }
      // CA2: el codigoInterno es el Document ID; la transacción garantiza que no se
      // sobrescriba una unidad existente (un set() sobre un ID existente sería un update).
      const ref = doc(this.db, COLECCION, maquina.codigoInterno);
      await runTransaction(this.db, async (tx) => {
        if ((await tx.get(ref)).exists()) throw new CodigoDuplicado();
        tx.set(ref, { ...maquina });
      });
      return exito(undefined);
    } catch (error: unknown) {
      if (error instanceof CodigoDuplicado) return fallo('MSG-09');
      return this.manejarError(error, maquina.codigoInterno, 'create');
    }
  }

  async actualizarMaquina(codigoInterno: string, cambios: CambiosMaquina): Promise<Resultado<void>> {
    try {
      if (cambios.placa && (await this.placaEnUso(cambios.placa, codigoInterno))) {
        return fallo('MSG-09');
      }
      await updateDoc(doc(this.db, COLECCION, codigoInterno), { ...cambios });
      return exito(undefined);
    } catch (error: unknown) {
      return this.manejarError(error, codigoInterno, 'update');
    }
  }

  /**
   * Unicidad de placa por consulta previa. Limitación conocida: el SDK web no admite
   * consultas dentro de transacciones, por lo que dos altas simultáneas con la misma
   * placa podrían pasar ambas (riesgo bajo: solo el admin registra flota).
   */
  private async placaEnUso(placa: string, codigoPropio: string): Promise<boolean> {
    const resultado = await getDocs(
      query(collection(this.db, COLECCION), where('placa', '==', placa), limit(2))
    );
    return resultado.docs.some((d) => d.id !== codigoPropio);
  }

  private async manejarError(
    error: unknown,
    documentoId: string,
    operacion: OperacionRechazada
  ): Promise<Resultado<void>> {
    if (esPermisoDenegado(error)) {
      await this.rechazos.registrar(COLECCION, documentoId, operacion);
    }
    return fallo(mapearErrorServidor(error));
  }
}
