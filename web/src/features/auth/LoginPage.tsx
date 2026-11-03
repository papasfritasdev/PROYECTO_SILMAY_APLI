import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { textoMensaje, type CodigoMensaje } from '../../domain/constants/messages';
import { useAuth } from '../../hooks/AuthContext';

/**
 * Pantalla de inicio de sesión web (HU-01b). Solo muestra textos del catálogo normalizado.
 */
export function LoginPage() {
  const { estado, iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<CodigoMensaje | null>(null);

  if (estado.tipo === 'autenticada') {
    return <Navigate to="/" replace />;
  }

  const puedeEnviar = email.trim().length > 0 && contrasena.length > 0 && !cargando;

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>): Promise<void> {
    evento.preventDefault();
    if (!puedeEnviar) return;

    setCargando(true);
    setError(null);
    const resultado = await iniciarSesion(email, contrasena);
    setCargando(false);

    if (resultado.ok) {
      navigate('/', { replace: true });
    } else {
      setError(resultado.mensaje);
    }
  }

  return (
    <main className="login">
      <form className="login__tarjeta" onSubmit={(e) => void manejarEnvio(e)} noValidate>
        <h1 className="login__marca">SILMAY</h1>
        <p className="login__subtitulo">Plataforma Administrativa</p>

        <label className="campo">
          <span>Correo electrónico</span>
          <input
            type="email"
            autoComplete="username"
            value={email}
            disabled={cargando}
            onChange={(e) => {
              setEmail(e.target.value);
              setError(null);
            }}
          />
        </label>

        <label className="campo">
          <span>Contraseña</span>
          <input
            type="password"
            autoComplete="current-password"
            value={contrasena}
            disabled={cargando}
            onChange={(e) => {
              setContrasena(e.target.value);
              setError(null);
            }}
          />
        </label>

        {error && (
          <p className="alerta alerta--error" role="alert">
            {textoMensaje(error)}
          </p>
        )}

        <button type="submit" className="boton boton--primario" disabled={!puedeEnviar}>
          {cargando ? 'Verificando…' : 'Iniciar Sesión'}
        </button>
      </form>
    </main>
  );
}
