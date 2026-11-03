import type { Rol } from './Rol';

/** Perfil completo de `usuarios` (docs/firebase-schema.md §2.1). */
export interface Usuario {
  readonly uid: string;
  readonly nombres: string;
  readonly cedula: string;
  readonly telefono: string;
  readonly email: string;
  readonly rol: Rol;
  readonly tipoLicencia: string | null;
  readonly zonaAsignada: string;
  readonly cargo: string;
  readonly estado: boolean;
}

/**
 * Payload de alta/edición para la Cloud Function `gestionarUsuario`
 * (docs/cloud-functions.md §2.2). Sin `uid` = alta; con `uid` = actualización.
 */
export interface DatosUsuario {
  readonly uid?: string;
  readonly nombres: string;
  readonly cedula: string;
  readonly telefono: string;
  readonly email: string;
  readonly password?: string;
  readonly rol: Rol;
  readonly tipoLicencia: string | null;
  readonly zonaAsignada: string;
  readonly cargo: string;
  readonly estado: boolean;
}
