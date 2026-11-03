import { collection, onSnapshot, orderBy, query, type Firestore } from 'firebase/firestore';
import { httpsCallable, type Functions } from 'firebase/functions';
import type { CodigoMensaje } from '../../domain/constants/messages';
import { exito, fallo, type Resultado } from '../../domain/model/Resultado';
import type { DatosUsuario, Usuario } from '../../domain/model/Usuario';
import type { UsuariosRepository } from '../../domain/repository/UsuariosRepository';
import { mapearErrorServidor } from '../errores';
import { mapearUsuario } from '../mapper/usuarioMapper';

interface RespuestaGestionarUsuario {
  success: boolean;
  uid: string;
  message: string;
}

/**
 * Lectura reactiva de `usuarios` (permitida al admin por las Security Rules) y escritura
 * exclusiva a través de la callable `gestionarUsuario` (claims + auditoría en servidor).
 */
export class FirebaseUsuariosRepository implements UsuariosRepository {
  constructor(
    private readonly db: Firestore,
    private readonly functions: Functions
  ) {}

  observarUsuarios(
    alRecibir: (usuarios: readonly Usuario[]) => void,
    alFallar: (mensaje: CodigoMensaje) => void
  ): () => void {
    return onSnapshot(
      query(collection(this.db, 'usuarios'), orderBy('nombres')),
      (snapshot) => {
        const usuarios = snapshot.docs
          .map((d) => mapearUsuario(d.id, d.data()))
          .filter((u): u is Usuario => u !== null);
        alRecibir(usuarios);
      },
      (error) => alFallar(mapearErrorServidor(error))
    );
  }

  async guardarUsuario(datos: DatosUsuario): Promise<Resultado<string>> {
    const gestionarUsuario = httpsCallable<DatosUsuario, RespuestaGestionarUsuario>(
      this.functions,
      'gestionarUsuario'
    );
    try {
      const respuesta = await gestionarUsuario(datos);
      return exito(respuesta.data.uid);
    } catch (error: unknown) {
      // HU-02 CA2: duplicidad de correo o cédula → MSG-17.
      return fallo(mapearErrorServidor(error, 'MSG-17'));
    }
  }
}
