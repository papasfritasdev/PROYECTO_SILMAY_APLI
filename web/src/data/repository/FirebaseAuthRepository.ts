import { FirebaseError } from 'firebase/app';
import { signInWithEmailAndPassword, signOut, type Auth, type User } from 'firebase/auth';
import { doc, getDoc, type Firestore } from 'firebase/firestore';
import type { CodigoMensaje } from '../../domain/constants/messages';
import { exito, fallo, type Resultado } from '../../domain/model/Resultado';
import type { UsuarioSesion } from '../../domain/model/UsuarioSesion';
import type { AuthRepository } from '../../domain/repository/AuthRepository';
import { mapearUsuarioSesion } from '../mapper/usuarioMapper';

/**
 * Traduce errores de Firebase Auth al catálogo normalizado.
 * - auth/user-disabled (baja lógica de gestionarUsuario) → MSG-03.
 * - auth/network-request-failed → MSG-18 (no es un problema de credenciales).
 * - Resto (credenciales, usuario inexistente) → MSG-01 opaco.
 */
export function mapearErrorAuth(error: unknown): CodigoMensaje {
  if (error instanceof FirebaseError) {
    if (error.code === 'auth/user-disabled') return 'MSG-03';
    if (error.code === 'auth/network-request-failed') return 'MSG-18';
  }
  return 'MSG-01';
}

export class FirebaseAuthRepository implements AuthRepository {
  constructor(
    private readonly auth: Auth,
    private readonly db: Firestore
  ) {}

  async iniciarSesion(email: string, contrasena: string): Promise<Resultado<UsuarioSesion>> {
    try {
      const credencial = await signInWithEmailAndPassword(this.auth, email, contrasena);
      const sesion = await this.cargarPerfil(credencial.user);
      if (!sesion) {
        await signOut(this.auth);
        return fallo('MSG-01');
      }
      return exito(sesion);
    } catch (error: unknown) {
      return fallo(mapearErrorAuth(error));
    }
  }

  async cerrarSesion(): Promise<void> {
    await signOut(this.auth);
  }

  async obtenerSesionActual(): Promise<UsuarioSesion | null> {
    await this.auth.authStateReady();
    const usuario = this.auth.currentUser;
    if (!usuario) return null;
    try {
      return await this.cargarPerfil(usuario);
    } catch {
      return null;
    }
  }

  private async cargarPerfil(usuario: User): Promise<UsuarioSesion | null> {
    const token = await usuario.getIdTokenResult();
    const perfil = await getDoc(doc(this.db, 'usuarios', usuario.uid));
    if (!perfil.exists()) return null;
    return mapearUsuarioSesion(usuario.uid, perfil.data(), token.claims['role']);
  }
}
