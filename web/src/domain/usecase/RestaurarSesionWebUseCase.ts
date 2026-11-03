import type { UsuarioSesion } from '../model/UsuarioSesion';
import type { AuthRepository } from '../repository/AuthRepository';
import { evaluarAccesoWeb } from './IniciarSesionWebUseCase';

/**
 * Caso de Uso: Restaurar la sesión persistida al recargar la página.
 * Reaplica las reglas de HU-01b: una sesión dada de baja o de rol de campo se descarta.
 */
export class RestaurarSesionWebUseCase {
  constructor(private readonly authRepository: AuthRepository) {}

  async ejecutar(): Promise<UsuarioSesion | null> {
    const sesion = await this.authRepository.obtenerSesionActual();
    if (!sesion) return null;

    if (evaluarAccesoWeb(sesion) !== null) {
      await this.authRepository.cerrarSesion();
      return null;
    }
    return sesion;
  }
}
