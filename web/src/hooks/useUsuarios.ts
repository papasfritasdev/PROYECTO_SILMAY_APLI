import { useEffect, useMemo, useState } from 'react';
import type { CodigoMensaje } from '../domain/constants/messages';
import type { Usuario } from '../domain/model/Usuario';
import { GuardarUsuarioUseCase } from '../domain/usecase/GuardarUsuarioUseCase';
import { useRepositorios } from './RepositoriosContext';

export interface EstadoUsuarios {
  readonly usuarios: readonly Usuario[];
  readonly cargando: boolean;
  readonly error: CodigoMensaje | null;
  readonly guardarUsuario: GuardarUsuarioUseCase;
}

/** Listado reactivo de usuarios + caso de uso de guardado (HU-02). */
export function useUsuarios(): EstadoUsuarios {
  const { usuarios: repositorio } = useRepositorios();
  const [usuarios, setUsuarios] = useState<readonly Usuario[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<CodigoMensaje | null>(null);

  useEffect(
    () =>
      repositorio.observarUsuarios(
        (lista) => {
          setUsuarios(lista);
          setCargando(false);
          setError(null);
        },
        (mensaje) => {
          setError(mensaje);
          setCargando(false);
        }
      ),
    [repositorio]
  );

  const guardarUsuario = useMemo(() => new GuardarUsuarioUseCase(repositorio), [repositorio]);

  return { usuarios, cargando, error, guardarUsuario };
}
