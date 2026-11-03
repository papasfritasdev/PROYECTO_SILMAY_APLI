import { useState } from 'react';
import { textoMensaje, type CodigoMensaje } from '../../domain/constants/messages';
import type { FormularioMaquina, Maquina } from '../../domain/model/Maquina';
import { useMaquinaria } from '../../hooks/useMaquinaria';
import { ETIQUETA_ESTADO_MAQUINA, ETIQUETA_TIPO_EQUIPO } from '../panel/etiquetas';
import { MaquinaFormulario } from './MaquinaFormulario';

type Edicion =
  | { readonly modo: 'cerrado' }
  | { readonly modo: 'alta' }
  | { readonly modo: 'edicion'; readonly maquina: Maquina };

const formatoHoras = (horas: number): string => `${horas.toFixed(1)} h`;

/** Gestión de maquinaria y volquetas (HU-09). Exclusiva del rol admin (ruta protegida). */
export function MaquinariaPage() {
  const { maquinas, cargando, error, gestion } = useMaquinaria();
  const [edicion, setEdicion] = useState<Edicion>({ modo: 'cerrado' });
  const [errorAccion, setErrorAccion] = useState<CodigoMensaje | null>(null);
  const [enProceso, setEnProceso] = useState<string | null>(null);

  async function registrar(formulario: FormularioMaquina) {
    const resultado = await gestion.registrar(formulario);
    if (resultado.ok) setEdicion({ modo: 'cerrado' });
    return resultado;
  }

  async function editar(maquina: Maquina, cambios: { placa: string; marca: string; modelo: string }) {
    const resultado = await gestion.editar(maquina, cambios);
    if (resultado.ok) setEdicion({ modo: 'cerrado' });
    return resultado;
  }

  async function alternarActividad(maquina: Maquina): Promise<void> {
    setEnProceso(maquina.codigoInterno);
    setErrorAccion(null);
    const resultado = await gestion.cambiarActividad(maquina, maquina.estado === 'inactiva');
    setEnProceso(null);
    if (!resultado.ok && resultado.mensaje) setErrorAccion(resultado.mensaje);
  }

  const mensaje = error ?? errorAccion;

  return (
    <section>
      <div className="encabezado-seccion">
        <h2>Maquinaria y volquetas</h2>
        {edicion.modo === 'cerrado' && (
          <button
            type="button"
            className="boton boton--primario boton--auto"
            onClick={() => setEdicion({ modo: 'alta' })}
          >
            Nueva unidad
          </button>
        )}
      </div>

      {edicion.modo !== 'cerrado' && (
        <MaquinaFormulario
          key={edicion.modo === 'edicion' ? edicion.maquina.codigoInterno : 'alta'}
          maquina={edicion.modo === 'edicion' ? edicion.maquina : undefined}
          onRegistrar={registrar}
          onEditar={editar}
          onCancelar={() => setEdicion({ modo: 'cerrado' })}
        />
      )}

      {mensaje && (
        <p className="alerta alerta--error" role="alert">
          {textoMensaje(mensaje)}
        </p>
      )}

      {cargando ? (
        <p className="cargando" role="status">
          Cargando flota…
        </p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Código</th>
              <th>Tipo</th>
              <th>Placa</th>
              <th>Marca / Modelo</th>
              <th>Horómetro actual</th>
              <th>Próximo mantenimiento</th>
              <th>Estado</th>
              <th aria-label="Acciones" />
            </tr>
          </thead>
          <tbody>
            {maquinas.map((maquina) => (
              <tr key={maquina.codigoInterno}>
                <td>{maquina.codigoInterno}</td>
                <td>{ETIQUETA_TIPO_EQUIPO[maquina.tipoEquipo]}</td>
                <td>{maquina.placa ?? '—'}</td>
                <td>
                  {maquina.marca} {maquina.modelo}
                </td>
                <td>{formatoHoras(maquina.horometroActual)}</td>
                <td>{formatoHoras(maquina.proximoMantenimientoHoras)}</td>
                <td>
                  <span className={`estado ${maquina.estado === 'inactiva' ? 'estado--inactivo' : 'estado--activo'}`}>
                    {ETIQUETA_ESTADO_MAQUINA[maquina.estado]}
                  </span>
                </td>
                <td className="tabla__acciones">
                  <button type="button" className="boton" onClick={() => setEdicion({ modo: 'edicion', maquina })}>
                    Editar
                  </button>
                  <button
                    type="button"
                    className="boton"
                    disabled={enProceso === maquina.codigoInterno}
                    onClick={() => void alternarActividad(maquina)}
                  >
                    {maquina.estado === 'inactiva' ? 'Reactivar' : 'Dar de baja'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
