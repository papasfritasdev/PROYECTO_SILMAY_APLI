import type { CodigoMensaje } from '../domain/constants/messages';
import type { Rol } from '../domain/model/Rol';
import { exito, fallo, type Resultado } from '../domain/model/Resultado';
import type { UsuarioSesion } from '../domain/model/UsuarioSesion';
import type { AuthRepository } from '../domain/repository/AuthRepository';

export function sesionDePrueba(rol: Rol, estado = true): UsuarioSesion {
  return {
    uid: `uid-${rol}`,
    email: `${rol}@silmay.ec`,
    nombres: `Usuario ${rol}`,
    rol,
    zonaAsignada: rol === 'operador' || rol === 'supervisor' ? 'cantera-banos' : 'administracion',
    estado
  };
}

/** Doble de prueba del AuthRepository (sin Firebase). */
export class FakeAuthRepository implements AuthRepository {
  cierresDeSesion = 0;
  ultimasCredenciales: { email: string; contrasena: string } | null = null;

  constructor(
    private respuestaLogin: Resultado<UsuarioSesion> = fallo('MSG-01'),
    private sesionPersistida: UsuarioSesion | null = null
  ) {}

  static conLogin(sesion: UsuarioSesion): FakeAuthRepository {
    return new FakeAuthRepository(exito(sesion));
  }

  static conError(mensaje: CodigoMensaje): FakeAuthRepository {
    return new FakeAuthRepository(fallo(mensaje));
  }

  static conSesionPersistida(sesion: UsuarioSesion): FakeAuthRepository {
    return new FakeAuthRepository(fallo('MSG-01'), sesion);
  }

  async iniciarSesion(email: string, contrasena: string): Promise<Resultado<UsuarioSesion>> {
    this.ultimasCredenciales = { email, contrasena };
    if (this.respuestaLogin.ok) {
      this.sesionPersistida = this.respuestaLogin.data;
    }
    return this.respuestaLogin;
  }

  async cerrarSesion(): Promise<void> {
    this.cierresDeSesion++;
    this.sesionPersistida = null;
  }

  async obtenerSesionActual(): Promise<UsuarioSesion | null> {
    return this.sesionPersistida;
  }
}
