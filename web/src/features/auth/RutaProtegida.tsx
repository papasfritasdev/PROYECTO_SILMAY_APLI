import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { puedeAccederModulo, type ModuloWeb } from '../../domain/navigation/modulosPorRol';
import { useAuth } from '../../hooks/AuthContext';

interface RutaProtegidaProps {
  /** Si se indica, además de la sesión exige que el rol tenga acceso al módulo (RBAC web). */
  readonly modulo?: ModuloWeb;
  readonly children: ReactNode;
}

/**
 * Guard de rutas (HT-08): sin sesión redirige a /login; sin permiso sobre el módulo, al inicio.
 * La autorización real la imponen las Security Rules; este guard evita exponer UI no autorizada.
 */
export function RutaProtegida({ modulo, children }: RutaProtegidaProps) {
  const { estado } = useAuth();

  if (estado.tipo === 'verificando') {
    return (
      <p className="cargando" role="status">
        Verificando sesión…
      </p>
    );
  }

  if (estado.tipo === 'sin-sesion') {
    return <Navigate to="/login" replace />;
  }

  if (modulo && !puedeAccederModulo(estado.sesion.rol, modulo)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
