import type { CodigoMensaje } from '../domain/constants/messages';
import type { CambiosMaquina, Maquina } from '../domain/model/Maquina';
import { exito, fallo, type Resultado } from '../domain/model/Resultado';
import type { MaquinariaRepository } from '../domain/repository/MaquinariaRepository';

export function maquinaDePrueba(cambios: Partial<Maquina> = {}): Maquina {
  return {
    codigoInterno: 'EX-01',
    placa: null,
    marca: 'Caterpillar',
    modelo: '320D',
    tipoEquipo: 'excavadora',
    horometroBase: 1000,
    horometroActual: 1100,
    intervaloMantenimiento: 250,
    proximoMantenimientoHoras: 1250,
    estado: 'operativa',
    ...cambios
  };
}

/** Doble en memoria que emula la unicidad por código (transacción) y por placa. */
export class FakeMaquinariaRepository implements MaquinariaRepository {
  readonly registradas: Maquina[] = [];
  readonly actualizaciones: { codigo: string; cambios: CambiosMaquina }[] = [];
  errorForzado: CodigoMensaje | null = null;
  private suscriptor: ((maquinas: readonly Maquina[]) => void) | null = null;

  constructor(private maquinas: Maquina[] = []) {}

  observarMaquinaria(alRecibir: (maquinas: readonly Maquina[]) => void): () => void {
    this.suscriptor = alRecibir;
    alRecibir(this.maquinas);
    return () => {
      this.suscriptor = null;
    };
  }

  async registrarMaquina(maquina: Maquina): Promise<Resultado<void>> {
    if (this.errorForzado) return fallo(this.errorForzado);
    const duplicada = this.maquinas.some(
      (m) => m.codigoInterno === maquina.codigoInterno || (maquina.placa !== null && m.placa === maquina.placa)
    );
    if (duplicada) return fallo('MSG-09');
    this.registradas.push(maquina);
    this.publicar([...this.maquinas, maquina]);
    return exito(undefined);
  }

  async actualizarMaquina(codigo: string, cambios: CambiosMaquina): Promise<Resultado<void>> {
    if (this.errorForzado) return fallo(this.errorForzado);
    if (cambios.placa && this.maquinas.some((m) => m.codigoInterno !== codigo && m.placa === cambios.placa)) {
      return fallo('MSG-09');
    }
    this.actualizaciones.push({ codigo, cambios });
    this.publicar(this.maquinas.map((m) => (m.codigoInterno === codigo ? { ...m, ...cambios } : m)));
    return exito(undefined);
  }

  private publicar(maquinas: Maquina[]): void {
    this.maquinas = maquinas;
    this.suscriptor?.(maquinas);
  }
}
