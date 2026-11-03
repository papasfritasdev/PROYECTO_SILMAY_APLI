import type { CodigoMensaje } from '../constants/messages';
import { estadoAlReactivar } from '../fleet/flota';
import type { FormularioMaquina, Maquina } from '../model/Maquina';
import type { MaquinariaRepository } from '../repository/MaquinariaRepository';
import { construirMaquina, validarCambios, type CampoMaquina } from '../validation/validarMaquina';

export type ResultadoMaquina =
  | { readonly ok: true }
  | {
      readonly ok: false;
      readonly camposInvalidos: readonly CampoMaquina[];
      readonly mensaje: CodigoMensaje | null;
    };

const invalido = (camposInvalidos: readonly CampoMaquina[]): ResultadoMaquina => ({
  ok: false,
  camposInvalidos,
  mensaje: null
});

/** Casos de Uso de flota (HU-09): alta, edición, baja lógica y reactivación. */
export class GestionarMaquinariaUseCase {
  constructor(private readonly repositorio: MaquinariaRepository) {}

  /** CA1 + CA2. */
  async registrar(formulario: FormularioMaquina): Promise<ResultadoMaquina> {
    const construccion = construirMaquina(formulario);
    if (!construccion.ok) return invalido(construccion.camposInvalidos);
    return this.aResultado(await this.repositorio.registrarMaquina(construccion.maquina));
  }

  async editar(
    maquina: Maquina,
    cambios: { readonly placa: string; readonly marca: string; readonly modelo: string }
  ): Promise<ResultadoMaquina> {
    const validacion = validarCambios(maquina, cambios);
    if (!validacion.ok) return invalido(validacion.camposInvalidos);
    return this.aResultado(
      await this.repositorio.actualizarMaquina(maquina.codigoInterno, {
        ...validacion.cambios,
        estado: maquina.estado
      })
    );
  }

  /** Baja lógica (`inactiva`) o reactivación según el horómetro (prohibido el borrado físico). */
  async cambiarActividad(maquina: Maquina, activa: boolean): Promise<ResultadoMaquina> {
    return this.aResultado(
      await this.repositorio.actualizarMaquina(maquina.codigoInterno, {
        placa: maquina.placa,
        marca: maquina.marca,
        modelo: maquina.modelo,
        estado: activa ? estadoAlReactivar(maquina) : 'inactiva'
      })
    );
  }

  private aResultado(resultado: { ok: true } | { ok: false; mensaje: CodigoMensaje }): ResultadoMaquina {
    return resultado.ok ? { ok: true } : { ok: false, camposInvalidos: [], mensaje: resultado.mensaje };
  }
}
