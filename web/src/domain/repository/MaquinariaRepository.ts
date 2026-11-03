import type { CodigoMensaje } from '../constants/messages';
import type { CambiosMaquina, Maquina } from '../model/Maquina';
import type { Resultado } from '../model/Resultado';

/** Contrato de gestión de flota (HU-09). */
export interface MaquinariaRepository {
  observarMaquinaria(
    alRecibir: (maquinas: readonly Maquina[]) => void,
    alFallar: (mensaje: CodigoMensaje) => void
  ): () => void;
  /** Alta atómica por código; MSG-09 si el código o la placa ya existen. */
  registrarMaquina(maquina: Maquina): Promise<Resultado<void>>;
  /** Edición o baja lógica; MSG-09 si la placa pertenece a otra unidad. */
  actualizarMaquina(codigoInterno: string, cambios: CambiosMaquina): Promise<Resultado<void>>;
}
