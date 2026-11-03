import type { Rol } from './Rol';

/** Sesión activa de un usuario en la plataforma web (M7). TypeScript puro. */
export interface UsuarioSesion {
  readonly uid: string;
  readonly email: string;
  readonly nombres: string;
  readonly rol: Rol;
  readonly zonaAsignada: string;
  readonly estado: boolean;
}
