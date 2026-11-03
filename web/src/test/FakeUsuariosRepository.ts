import type { CodigoMensaje } from '../domain/constants/messages';
import { exito, fallo, type Resultado } from '../domain/model/Resultado';
import type { DatosUsuario, Usuario } from '../domain/model/Usuario';
import type { UsuariosRepository } from '../domain/repository/UsuariosRepository';

export function usuarioDePrueba(cambios: Partial<Usuario> = {}): Usuario {
  return {
    uid: 'uid-op',
    nombres: 'Carlos Operador',
    cedula: '0922879580',
    telefono: '0988888888',
    email: 'operador@silmay.ec',
    rol: 'operador',
    tipoLicencia: 'Tipo G',
    zonaAsignada: 'cantera-banos',
    cargo: 'Operador de Maquinaria',
    estado: true,
    ...cambios
  };
}

/** Doble de prueba en memoria que emula a gestionarUsuario + listado reactivo. */
export class FakeUsuariosRepository implements UsuariosRepository {
  readonly guardados: DatosUsuario[] = [];
  errorAlGuardar: CodigoMensaje | null = null;
  private suscriptor: ((usuarios: readonly Usuario[]) => void) | null = null;

  constructor(private usuarios: Usuario[] = []) {}

  observarUsuarios(alRecibir: (usuarios: readonly Usuario[]) => void): () => void {
    this.suscriptor = alRecibir;
    alRecibir(this.usuarios);
    return () => {
      this.suscriptor = null;
    };
  }

  async guardarUsuario(datos: DatosUsuario): Promise<Resultado<string>> {
    this.guardados.push(datos);
    if (this.errorAlGuardar) return fallo(this.errorAlGuardar);

    const uid = datos.uid ?? `uid-${this.guardados.length}`;
    const { password: _omitida, ...perfil } = datos;
    const actualizado: Usuario = { ...perfil, uid };
    this.usuarios = [...this.usuarios.filter((u) => u.uid !== uid), actualizado];
    this.suscriptor?.(this.usuarios);
    return exito(uid);
  }
}
