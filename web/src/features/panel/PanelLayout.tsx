import { NavLink, Outlet } from 'react-router-dom';
import { modulosPorRol } from '../../domain/navigation/modulosPorRol';
import { useAuth } from '../../hooks/AuthContext';
import { ETIQUETA_MODULO, ETIQUETA_ROL } from './etiquetas';

/**
 * Panel principal (HU-01b CA1): muestra únicamente los módulos autorizados para el rol.
 */
export function PanelLayout() {
  const { estado, cerrarSesion } = useAuth();
  if (estado.tipo !== 'autenticada') return null;
  const { sesion } = estado;

  return (
    <div className="panel">
      <header className="panel__cabecera">
        <span className="panel__marca">SILMAY</span>
        <div className="panel__usuario">
          <span>{sesion.nombres}</span>
          <span className="etiqueta-rol">{ETIQUETA_ROL[sesion.rol]}</span>
          <button type="button" className="boton" onClick={() => void cerrarSesion()}>
            Cerrar sesión
          </button>
        </div>
      </header>

      <nav className="panel__menu" aria-label="Módulos">
        <NavLink to="/" end>
          Inicio
        </NavLink>
        {modulosPorRol(sesion.rol).map((modulo) => (
          <NavLink key={modulo} to={`/${modulo}`}>
            {ETIQUETA_MODULO[modulo]}
          </NavLink>
        ))}
      </nav>

      <main className="panel__contenido">
        <Outlet />
      </main>
    </div>
  );
}
