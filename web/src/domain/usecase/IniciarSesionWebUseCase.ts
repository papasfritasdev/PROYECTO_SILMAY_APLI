import { habilitadoEnWeb } from '../model/Rol';
import { exito, fallo, type Resultado } from '../model/Resultado';
import type { UsuarioSesion } from '../model/UsuarioSesion';
import type { AuthRepository } from '../repository/AuthRepository';

/**
 * Caso de Uso: Inicio de sesión por rol en la plataforma web (HU-01b).
 * - CA1: contador y admin activos acceden.
 * - CA2: operador y supervisor son bloqueados con MSG-13 y su sesión se cierra.
 * - Baja lógica (HU-02 CA3): estado false → MSG-03 y cierre de sesión.
 * - Campos vacíos → MSG-01 (mensaje opaco).
 */
export class IniciarSesionWebUseCase {
  constructor(private readonly authRepository: AuthRepository) {}

  async ejecutar(email: string, contrasena: string): Promise<Resultado<UsuarioSesion>> {
    const emailLimpio = email.trim();
    if (emailLimpio.length === 0 || contrasena.length === 0) {
      return fallo('MSG-01');
    }

    const resultado = await this.authRepository.iniciarSesion(emailLimpio, contrasena);
    if (!resultado.ok) {
      return resultado;
    }

    const rechazo = evaluarAccesoWeb(resultado.data);
    if (rechazo) {
      await this.authRepository.cerrarSesion();
      return fallo(rechazo);
    }
    return exito(resultado.data);
  }
}

/** Reglas de acceso web compartidas por login y restauración de sesión. */
export function evaluarAccesoWeb(sesion: UsuarioSesion): 'MSG-03' | 'MSG-13' | null {
  if (!sesion.estado) return 'MSG-03';
  if (!habilitadoEnWeb(sesion.rol)) return 'MSG-13';
  return null;
}
