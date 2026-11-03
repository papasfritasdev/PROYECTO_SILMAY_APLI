import { useAuth } from '../../hooks/AuthContext';
import { ETIQUETA_ROL } from './etiquetas';

export function InicioPage() {
  const { estado } = useAuth();
  if (estado.tipo !== 'autenticada') return null;

  return (
    <section>
      <h2>Bienvenido, {estado.sesion.nombres}</h2>
      <p>
        Perfil: {ETIQUETA_ROL[estado.sesion.rol]} · {estado.sesion.email}
      </p>
      <p>Seleccione un módulo del menú para continuar.</p>
    </section>
  );
}
