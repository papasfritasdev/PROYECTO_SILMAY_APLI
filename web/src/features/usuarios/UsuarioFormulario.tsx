import { useId, useState, type FormEvent } from 'react';
import { textoMensaje, type CodigoMensaje } from '../../domain/constants/messages';
import { ROLES, type Rol } from '../../domain/model/Rol';
import type { DatosUsuario } from '../../domain/model/Usuario';
import {
  camposBloqueadosParaAutor,
  type ResultadoGuardado
} from '../../domain/usecase/GuardarUsuarioUseCase';
import { validarUsuario, type CampoUsuario } from '../../domain/validation/validarUsuario';
import { ETIQUETA_ROL } from '../panel/etiquetas';

type CampoTexto = Exclude<CampoUsuario, 'rol' | 'estado'> | 'tipoLicencia';

interface DefinicionCampo {
  readonly campo: CampoTexto;
  readonly etiqueta: string;
  readonly ayuda: string;
  readonly tipo?: 'text' | 'email' | 'tel' | 'password';
}

/** Ayudas fijas de formato (decisión del equipo: sin mensajes de validación fuera del catálogo). */
function definicionCampos(esAlta: boolean): readonly DefinicionCampo[] {
  return [
    { campo: 'nombres', etiqueta: 'Nombres', ayuda: 'Nombres y apellidos completos' },
    { campo: 'cedula', etiqueta: 'Cédula', ayuda: 'Cédula ecuatoriana · 10 dígitos' },
    { campo: 'telefono', etiqueta: 'Teléfono', ayuda: 'Celular 09XXXXXXXX o fijo 0XXXXXXXX (admite +593)', tipo: 'tel' },
    { campo: 'email', etiqueta: 'Correo electrónico', ayuda: 'Correo institucional', tipo: 'email' },
    {
      campo: 'password',
      etiqueta: 'Contraseña',
      ayuda: esAlta
        ? 'Mínimo 8 caracteres con letras y números'
        : 'Opcional · dejar vacío para mantener la actual',
      tipo: 'password'
    },
    { campo: 'zonaAsignada', etiqueta: 'Zona asignada', ayuda: 'Frente operativo, p. ej. cantera-banos' },
    { campo: 'cargo', etiqueta: 'Cargo', ayuda: 'Puesto de trabajo' },
    { campo: 'tipoLicencia', etiqueta: 'Tipo de licencia', ayuda: 'Opcional' }
  ];
}

interface UsuarioFormularioProps {
  readonly inicial: DatosUsuario;
  readonly autorUid: string;
  readonly onGuardar: (datos: DatosUsuario) => Promise<ResultadoGuardado>;
  readonly onCancelar: () => void;
}

export function UsuarioFormulario({ inicial, autorUid, onGuardar, onCancelar }: UsuarioFormularioProps) {
  const idBase = useId();
  const esAlta = inicial.uid === undefined;
  const esPropioPerfil = inicial.uid === autorUid;
  const [datos, setDatos] = useState<DatosUsuario>(inicial);
  const [tocados, setTocados] = useState<ReadonlySet<CampoUsuario | 'tipoLicencia'>>(new Set());
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<CodigoMensaje | null>(null);

  const invalidos = new Set<CampoUsuario>([
    ...validarUsuario(datos),
    ...camposBloqueadosParaAutor(datos, autorUid)
  ]);
  const puedeGuardar = invalidos.size === 0 && !guardando;

  function actualizar(campo: CampoTexto, valor: string): void {
    setDatos((previo) => ({ ...previo, [campo]: valor }));
    setTocados((previo) => new Set(previo).add(campo));
    setError(null);
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>): Promise<void> {
    evento.preventDefault();
    if (!puedeGuardar) return;
    setGuardando(true);
    const resultado = await onGuardar(datos);
    setGuardando(false);
    if (!resultado.ok) {
      setError(resultado.mensaje);
    }
  }

  return (
    <form className="formulario" onSubmit={(e) => void manejarEnvio(e)} noValidate>
      <h3>{esAlta ? 'Nuevo usuario' : `Editar ${inicial.nombres}`}</h3>

      <div className="formulario__grilla">
        {definicionCampos(esAlta).map(({ campo, etiqueta, ayuda, tipo = 'text' }) => {
          const marcadoInvalido = campo !== 'tipoLicencia' && tocados.has(campo) && invalidos.has(campo);
          const valor = campo === 'tipoLicencia' ? (datos.tipoLicencia ?? '') : (datos[campo] ?? '');
          const id = `${idBase}-${campo}`;
          return (
            <div key={campo} className={`campo${marcadoInvalido ? ' campo--invalido' : ''}`}>
              <label htmlFor={id}>{etiqueta}</label>
              <input
                id={id}
                type={tipo}
                value={valor}
                aria-invalid={marcadoInvalido}
                aria-describedby={`${id}-ayuda`}
                autoComplete={campo === 'password' ? 'new-password' : 'off'}
                disabled={guardando}
                onChange={(e) => actualizar(campo, e.target.value)}
              />
              <small id={`${id}-ayuda`} className="campo__ayuda">
                {ayuda}
              </small>
            </div>
          );
        })}

        <div className="campo">
          <label htmlFor={`${idBase}-rol`}>Rol</label>
          <select
            id={`${idBase}-rol`}
            aria-describedby={`${idBase}-rol-ayuda`}
            value={datos.rol}
            disabled={guardando || esPropioPerfil}
            onChange={(e) => setDatos((previo) => ({ ...previo, rol: e.target.value as Rol }))}
          >
            {ROLES.map((rol) => (
              <option key={rol} value={rol}>
                {ETIQUETA_ROL[rol]}
              </option>
            ))}
          </select>
          <small id={`${idBase}-rol-ayuda`} className="campo__ayuda">
            {esPropioPerfil ? 'No puede cambiar su propio rol' : 'Operador y Supervisor usan la app móvil'}
          </small>
        </div>
      </div>

      {error && (
        <p className="alerta alerta--error" role="alert">
          {textoMensaje(error)}
        </p>
      )}

      <div className="formulario__acciones">
        <button type="button" className="boton" onClick={onCancelar} disabled={guardando}>
          Cancelar
        </button>
        <button type="submit" className="boton boton--primario" disabled={!puedeGuardar}>
          {guardando ? 'Guardando…' : 'Guardar'}
        </button>
      </div>
    </form>
  );
}
