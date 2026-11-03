import type { CodigoMensaje } from '../constants/messages';
import type { Resultado } from '../model/Resultado';
import type { DatosUsuario, Usuario } from '../model/Usuario';

/** Contrato de gestión de usuarios (HU-02). Lectura reactiva + escritura vía gestionarUsuario. */
export interface UsuariosRepository {
  /** Suscripción en tiempo real a `usuarios`. Retorna la función para cancelarla. */
  observarUsuarios(
    alRecibir: (usuarios: readonly Usuario[]) => void,
    alFallar: (mensaje: CodigoMensaje) => void
  ): () => void;
  /** Crea o actualiza; retorna el uid del usuario. */
  guardarUsuario(datos: DatosUsuario): Promise<Resultado<string>>;
}
