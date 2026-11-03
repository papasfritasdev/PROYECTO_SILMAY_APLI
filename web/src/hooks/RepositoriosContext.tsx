import { createContext, useContext, type ReactNode } from 'react';
import type { MaquinariaRepository } from '../domain/repository/MaquinariaRepository';
import type { UsuariosRepository } from '../domain/repository/UsuariosRepository';

/** Repositorios de dominio disponibles para las features (DI por contexto). */
export interface Repositorios {
  readonly usuarios: UsuariosRepository;
  readonly maquinaria: MaquinariaRepository;
}

const RepositoriosContext = createContext<Repositorios | null>(null);

export function RepositoriosProvider({
  repositorios,
  children
}: {
  readonly repositorios: Repositorios;
  readonly children: ReactNode;
}) {
  return <RepositoriosContext.Provider value={repositorios}>{children}</RepositoriosContext.Provider>;
}

export function useRepositorios(): Repositorios {
  const contexto = useContext(RepositoriosContext);
  if (!contexto) {
    throw new Error('useRepositorios debe usarse dentro de <RepositoriosProvider>.');
  }
  return contexto;
}
