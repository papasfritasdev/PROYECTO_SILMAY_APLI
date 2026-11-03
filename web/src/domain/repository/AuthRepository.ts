import type { Resultado } from '../model/Resultado';
import type { UsuarioSesion } from '../model/UsuarioSesion';

/**
 * Contrato de autenticación de la plataforma web.
 * La capa `data` lo implementa con Firebase; el dominio no conoce el SDK.
 */
export interface AuthRepository {
  iniciarSesion(email: string, contrasena: string): Promise<Resultado<UsuarioSesion>>;
  cerrarSesion(): Promise<void>;
  /** Sesión persistida por el navegador (recarga de página), o null si no hay. */
  obtenerSesionActual(): Promise<UsuarioSesion | null>;
}
