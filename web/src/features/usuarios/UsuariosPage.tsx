import { useState } from 'react';
import { textoMensaje, type CodigoMensaje } from '../../domain/constants/messages';
import type { DatosUsuario, Usuario } from '../../domain/model/Usuario';
import { datosDesdeUsuario } from '../../domain/usecase/GuardarUsuarioUseCase';
import { useAuth } from '../../hooks/AuthContext';
import { useUsuarios } from '../../hooks/useUsuarios';
import { ETIQUETA_ROL } from '../panel/etiquetas';
import { UsuarioFormulario } from './UsuarioFormulario';

const USUARIO_NUEVO: DatosUsuario = {
  nombres: '',
  cedula: '',
  telefono: '',
  email: '',
  password: '',
  rol: 'operador',
  tipoLicencia: null,
  zonaAsignada: '',
  cargo: '',
  estado: true
};

type Edicion = { readonly modo: 'cerrado' } | { readonly modo: 'abierto'; readonly datos: DatosUsuario };

/** Administración de usuarios y roles (HU-02). Exclusiva del rol admin (ruta protegida). */
export function UsuariosPage() {
  const { estado } = useAuth();
  const { usuarios, cargando, error, guardarUsuario } = useUsuarios();
  const [edicion, setEdicion] = useState<Edicion>({ modo: 'cerrado' });
  const [errorAccion, setErrorAccion] = useState<CodigoMensaje | null>(null);
  const [enProceso, setEnProceso] = useState<string | null>(null);

  if (estado.tipo !== 'autenticada') return null;
  const autorUid = estado.sesion.uid;

  async function guardar(datos: DatosUsuario) {
    const resultado = await guardarUsuario.ejecutar(datos, autorUid);
    if (resultado.ok) setEdicion({ modo: 'cerrado' });
    return resultado;
  }

  async function alternarEstado(usuario: Usuario): Promise<void> {
    setEnProceso(usuario.uid);
    setErrorAccion(null);
    const resultado = await guardarUsuario.cambiarEstado(usuario, !usuario.estado, autorUid);
    setEnProceso(null);
    if (!resultado.ok && resultado.mensaje) setErrorAccion(resultado.mensaje);
  }

  return (
    <section>
      <div className="encabezado-seccion">
        <h2>Usuarios</h2>
        {edicion.modo === 'cerrado' && (
          <button
            type="button"
            className="boton boton--primario boton--auto"
            onClick={() => setEdicion({ modo: 'abierto', datos: USUARIO_NUEVO })}
          >
            Nuevo usuario
          </button>
        )}
      </div>

      {edicion.modo === 'abierto' && (
        <UsuarioFormulario
          key={edicion.datos.uid ?? 'nuevo'}
          inicial={edicion.datos}
          autorUid={autorUid}
          onGuardar={guardar}
          onCancelar={() => setEdicion({ modo: 'cerrado' })}
        />
      )}

      {(error ?? errorAccion) && (
        <p className="alerta alerta--error" role="alert">
          {textoMensaje((error ?? errorAccion) as CodigoMensaje)}
        </p>
      )}

      {cargando ? (
        <p className="cargando" role="status">
          Cargando usuarios…
        </p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Nombres</th>
              <th>Cédula</th>
              <th>Correo</th>
              <th>Rol</th>
              <th>Zona</th>
              <th>Estado</th>
              <th aria-label="Acciones" />
            </tr>
          </thead>
          <tbody>
            {usuarios.map((usuario) => {
              const esPropio = usuario.uid === autorUid;
              return (
                <tr key={usuario.uid}>
                  <td>{usuario.nombres}</td>
                  <td>{usuario.cedula}</td>
                  <td>{usuario.email}</td>
                  <td>{ETIQUETA_ROL[usuario.rol]}</td>
                  <td>{usuario.zonaAsignada}</td>
                  <td>
                    <span className={`estado ${usuario.estado ? 'estado--activo' : 'estado--inactivo'}`}>
                      {usuario.estado ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td className="tabla__acciones">
                    <button
                      type="button"
                      className="boton"
                      onClick={() =>
                        setEdicion({ modo: 'abierto', datos: datosDesdeUsuario(usuario, { password: '' }) })
                      }
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="boton"
                      disabled={esPropio || enProceso === usuario.uid}
                      title={esPropio ? 'No puede dar de baja su propia cuenta' : undefined}
                      onClick={() => void alternarEstado(usuario)}
                    >
                      {usuario.estado ? 'Dar de baja' : 'Reactivar'}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </section>
  );
}
