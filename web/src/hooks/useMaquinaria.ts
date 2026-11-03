import { useEffect, useMemo, useState } from 'react';
import type { CodigoMensaje } from '../domain/constants/messages';
import type { Maquina } from '../domain/model/Maquina';
import { GestionarMaquinariaUseCase } from '../domain/usecase/GestionarMaquinariaUseCase';
import { useRepositorios } from './RepositoriosContext';

export interface EstadoMaquinaria {
  readonly maquinas: readonly Maquina[];
  readonly cargando: boolean;
  readonly error: CodigoMensaje | null;
  readonly gestion: GestionarMaquinariaUseCase;
}

/** Listado reactivo de la flota + casos de uso (HU-09). */
export function useMaquinaria(): EstadoMaquinaria {
  const { maquinaria: repositorio } = useRepositorios();
  const [maquinas, setMaquinas] = useState<readonly Maquina[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<CodigoMensaje | null>(null);

  useEffect(
    () =>
      repositorio.observarMaquinaria(
        (lista) => {
          setMaquinas(lista);
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

  const gestion = useMemo(() => new GestionarMaquinariaUseCase(repositorio), [repositorio]);

  return { maquinas, cargando, error, gestion };
}
