import { FirebaseError } from 'firebase/app';
import type { CodigoMensaje } from '../domain/constants/messages';

/**
 * Traduce errores de Cloud Functions / Firestore al catálogo (docs/cloud-functions.md §2.2).
 * Los códigos llegan con prefijo `functions/` (callables) o sin prefijo (Firestore).
 */
export function mapearErrorServidor(
  error: unknown,
  duplicado: CodigoMensaje = 'MSG-18'
): CodigoMensaje {
  if (!(error instanceof FirebaseError)) return 'MSG-18';
  const codigo = error.code.replace(/^functions\//, '');
  switch (codigo) {
    case 'already-exists':
      return duplicado;
    case 'permission-denied':
    case 'unauthenticated':
      return 'MSG-13';
    default:
      return 'MSG-18';
  }
}

export function esPermisoDenegado(error: unknown): boolean {
  return (
    error instanceof FirebaseError &&
    error.code.replace(/^functions\//, '') === 'permission-denied'
  );
}
