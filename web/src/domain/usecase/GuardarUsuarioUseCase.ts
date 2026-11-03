import type { CodigoMensaje } from '../constants/messages';
import type { DatosUsuario, Usuario } from '../model/Usuario';
import type { UsuariosRepository } from '../repository/UsuariosRepository';
import { normalizarUsuario, validarUsuario, type CampoUsuario } from '../validation/validarUsuario';

export type ResultadoGuardado =
  | { readonly ok: true; readonly uid: string }
  | {
      readonly ok: false;
      readonly camposInvalidos: readonly CampoUsuario[];
      readonly mensaje: CodigoMensaje | null;
    };

/**
 * Regla de autoprotección: un administrador no puede darse de baja ni retirarse su rol
 * (el servidor también lo rechaza con failed-precondition).
 */
export function camposBloqueadosParaAutor(datos: DatosUsuario, autorUid: string): CampoUsuario[] {
  if (datos.uid !== autorUid) return [];
  const bloqueados: CampoUsuario[] = [];
  if (!datos.estado) bloqueados.push('estado');
  if (datos.rol !== 'admin') bloqueados.push('rol');
  return bloqueados;
}

/** Caso de Uso: alta, edición, baja lógica y reactivación de usuarios (HU-02). */
export class GuardarUsuarioUseCase {
  constructor(private readonly repositorio: UsuariosRepository) {}

  async ejecutar(datos: DatosUsuario, autorUid: string): Promise<ResultadoGuardado> {
    const camposInvalidos = [
      ...validarUsuario(datos),
      ...camposBloqueadosParaAutor(datos, autorUid)
    ];
    if (camposInvalidos.length > 0) {
      return { ok: false, camposInvalidos, mensaje: null };
    }

    const resultado = await this.repositorio.guardarUsuario(normalizarUsuario(datos));
    return resultado.ok
      ? { ok: true, uid: resultado.data }
      : { ok: false, camposInvalidos: [], mensaje: resultado.mensaje };
  }

  /** CA3: baja lógica (`false`) o reactivación (`true`) conservando el resto del perfil. */
  async cambiarEstado(usuario: Usuario, estado: boolean, autorUid: string): Promise<ResultadoGuardado> {
    return this.ejecutar(datosDesdeUsuario(usuario, { estado }), autorUid);
  }
}

export function datosDesdeUsuario(usuario: Usuario, cambios: Partial<DatosUsuario> = {}): DatosUsuario {
  return {
    uid: usuario.uid,
    nombres: usuario.nombres,
    cedula: usuario.cedula,
    telefono: usuario.telefono,
    email: usuario.email,
    rol: usuario.rol,
    tipoLicencia: usuario.tipoLicencia,
    zonaAsignada: usuario.zonaAsignada,
    cargo: usuario.cargo,
    estado: usuario.estado,
    ...cambios
  };
}
