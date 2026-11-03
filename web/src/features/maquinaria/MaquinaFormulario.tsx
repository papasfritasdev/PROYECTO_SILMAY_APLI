import { useId, useState, type FormEvent } from 'react';
import { textoMensaje, type CodigoMensaje } from '../../domain/constants/messages';
import { calcularProximoMantenimiento } from '../../domain/fleet/flota';
import { TIPOS_EQUIPO, type FormularioMaquina, type Maquina, type TipoEquipo } from '../../domain/model/Maquina';
import type { ResultadoMaquina } from '../../domain/usecase/GestionarMaquinariaUseCase';
import { construirMaquina, validarCambios, type CampoMaquina } from '../../domain/validation/validarMaquina';
import { ETIQUETA_TIPO_EQUIPO } from '../panel/etiquetas';

type CampoTexto = Exclude<CampoMaquina, 'tipoEquipo'>;

interface DefinicionCampo {
  readonly campo: CampoTexto;
  readonly etiqueta: string;
  readonly ayuda: string;
  /** En edición solo se modifican placa, marca y modelo. */
  readonly editable: boolean;
}

const CAMPOS: readonly DefinicionCampo[] = [
  { campo: 'codigoInterno', etiqueta: 'Código interno', ayuda: 'Código de flota, p. ej. EX-01 o VOL-03', editable: false },
  { campo: 'placa', etiqueta: 'Placa', ayuda: 'Formato ABC-1234 · obligatoria en volquetas', editable: true },
  { campo: 'marca', etiqueta: 'Marca', ayuda: 'Fabricante del equipo', editable: true },
  { campo: 'modelo', etiqueta: 'Modelo', ayuda: 'Modelo del equipo', editable: true },
  { campo: 'horometroBase', etiqueta: 'Horómetro base', ayuda: 'Horas al ingresar al sistema · máximo 1 decimal', editable: false },
  { campo: 'intervaloMantenimiento', etiqueta: 'Intervalo de mantenimiento', ayuda: 'Horas del ciclo preventivo, p. ej. 250 o 500', editable: false }
];

const FORMULARIO_VACIO: FormularioMaquina = {
  codigoInterno: '',
  placa: '',
  marca: '',
  modelo: '',
  tipoEquipo: 'excavadora',
  horometroBase: '',
  intervaloMantenimiento: ''
};

function desdeMaquina(maquina: Maquina): FormularioMaquina {
  return {
    codigoInterno: maquina.codigoInterno,
    placa: maquina.placa ?? '',
    marca: maquina.marca,
    modelo: maquina.modelo,
    tipoEquipo: maquina.tipoEquipo,
    horometroBase: String(maquina.horometroBase),
    intervaloMantenimiento: String(maquina.intervaloMantenimiento)
  };
}

interface MaquinaFormularioProps {
  /** Ausente = alta; presente = edición de esa unidad. */
  readonly maquina?: Maquina;
  readonly onRegistrar: (formulario: FormularioMaquina) => Promise<ResultadoMaquina>;
  readonly onEditar: (
    maquina: Maquina,
    cambios: { placa: string; marca: string; modelo: string }
  ) => Promise<ResultadoMaquina>;
  readonly onCancelar: () => void;
}

export function MaquinaFormulario({ maquina, onRegistrar, onEditar, onCancelar }: MaquinaFormularioProps) {
  const idBase = useId();
  const esAlta = maquina === undefined;
  const [datos, setDatos] = useState<FormularioMaquina>(maquina ? desdeMaquina(maquina) : FORMULARIO_VACIO);
  const [tocados, setTocados] = useState<ReadonlySet<CampoMaquina>>(new Set());
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<CodigoMensaje | null>(null);

  const validacion = maquina ? validarCambios(maquina, datos) : construirMaquina(datos);
  const invalidos = new Set<CampoMaquina>(validacion.ok ? [] : validacion.camposInvalidos);
  const puedeGuardar = validacion.ok && !guardando;

  const base = Number(datos.horometroBase.replace(',', '.'));
  const intervalo = Number(datos.intervaloMantenimiento);
  const proximo =
    datos.horometroBase.trim() && datos.intervaloMantenimiento.trim() && Number.isFinite(base + intervalo)
      ? calcularProximoMantenimiento(base, intervalo)
      : null;

  function actualizar(campo: CampoMaquina, valor: string): void {
    setDatos((previo) => ({ ...previo, [campo]: valor }));
    setTocados((previo) => new Set(previo).add(campo));
    setError(null);
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>): Promise<void> {
    evento.preventDefault();
    if (!puedeGuardar) return;
    setGuardando(true);
    const resultado = maquina ? await onEditar(maquina, datos) : await onRegistrar(datos);
    setGuardando(false);
    if (!resultado.ok) setError(resultado.mensaje);
  }

  return (
    <form className="formulario" onSubmit={(e) => void manejarEnvio(e)} noValidate>
      <h3>{esAlta ? 'Nueva unidad' : `Editar ${maquina.codigoInterno}`}</h3>

      <div className="formulario__grilla">
        <div className="campo">
          <label htmlFor={`${idBase}-tipo`}>Tipo de equipo</label>
          <select
            id={`${idBase}-tipo`}
            value={datos.tipoEquipo}
            disabled={guardando || !esAlta}
            onChange={(e) => {
              setDatos((previo) => ({ ...previo, tipoEquipo: e.target.value as TipoEquipo }));
              setError(null);
            }}
          >
            {TIPOS_EQUIPO.map((tipo) => (
              <option key={tipo} value={tipo}>
                {ETIQUETA_TIPO_EQUIPO[tipo]}
              </option>
            ))}
          </select>
        </div>

        {CAMPOS.map(({ campo, etiqueta, ayuda, editable }) => {
          const id = `${idBase}-${campo}`;
          const marcado = tocados.has(campo) && invalidos.has(campo);
          return (
            <div key={campo} className={`campo${marcado ? ' campo--invalido' : ''}`}>
              <label htmlFor={id}>{etiqueta}</label>
              <input
                id={id}
                value={datos[campo]}
                inputMode={campo === 'horometroBase' || campo === 'intervaloMantenimiento' ? 'decimal' : undefined}
                aria-invalid={marcado}
                aria-describedby={`${id}-ayuda`}
                disabled={guardando || (!esAlta && !editable)}
                onChange={(e) => actualizar(campo, e.target.value)}
              />
              <small id={`${id}-ayuda`} className="campo__ayuda">
                {ayuda}
              </small>
            </div>
          );
        })}
      </div>

      <p className="formulario__calculo" aria-live="polite">
        Próximo mantenimiento: <strong>{proximo !== null ? `${proximo} h` : '—'}</strong>
      </p>

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
