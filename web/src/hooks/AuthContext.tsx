import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from 'react';
import type { Resultado } from '../domain/model/Resultado';
import type { UsuarioSesion } from '../domain/model/UsuarioSesion';
import type { AuthRepository } from '../domain/repository/AuthRepository';
import { IniciarSesionWebUseCase } from '../domain/usecase/IniciarSesionWebUseCase';
import { RestaurarSesionWebUseCase } from '../domain/usecase/RestaurarSesionWebUseCase';
import { useInactividad, type OpcionesInactividad } from './useInactividad';

export type EstadoSesion =
  | { readonly tipo: 'verificando' }
  | { readonly tipo: 'sin-sesion' }
  | { readonly tipo: 'autenticada'; readonly sesion: UsuarioSesion };

export interface AuthContextValue {
  readonly estado: EstadoSesion;
  iniciarSesion(email: string, contrasena: string): Promise<Resultado<UsuarioSesion>>;
  cerrarSesion(): Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

interface AuthProviderProps {
  readonly repository: AuthRepository;
  readonly inactividad?: OpcionesInactividad;
  readonly children: ReactNode;
}

/**
 * Proveedor de sesión de la plataforma web (M7, HU-01b).
 * Inyecta el AuthRepository (DI por contexto) y aplica el cierre por inactividad de 30 min.
 */
export function AuthProvider({ repository, inactividad, children }: AuthProviderProps) {
  const [estado, setEstado] = useState<EstadoSesion>({ tipo: 'verificando' });

  const iniciarSesionUseCase = useMemo(() => new IniciarSesionWebUseCase(repository), [repository]);
  const restaurarSesionUseCase = useMemo(
    () => new RestaurarSesionWebUseCase(repository),
    [repository]
  );

  useEffect(() => {
    let vigente = true;
    void restaurarSesionUseCase.ejecutar().then((sesion) => {
      if (!vigente) return;
      setEstado(sesion ? { tipo: 'autenticada', sesion } : { tipo: 'sin-sesion' });
    });
    return () => {
      vigente = false;
    };
  }, [restaurarSesionUseCase]);

  const iniciarSesion = useCallback(
    async (email: string, contrasena: string) => {
      const resultado = await iniciarSesionUseCase.ejecutar(email, contrasena);
      if (resultado.ok) {
        setEstado({ tipo: 'autenticada', sesion: resultado.data });
      }
      return resultado;
    },
    [iniciarSesionUseCase]
  );

  const cerrarSesion = useCallback(async () => {
    await repository.cerrarSesion();
    setEstado({ tipo: 'sin-sesion' });
  }, [repository]);

  // HU-01b CA3: cierre automático tras 30 minutos sin interacción.
  useInactividad(estado.tipo === 'autenticada', () => void cerrarSesion(), inactividad);

  const valor = useMemo<AuthContextValue>(
    () => ({ estado, iniciarSesion, cerrarSesion }),
    [estado, iniciarSesion, cerrarSesion]
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext);
  if (!contexto) {
    throw new Error('useAuth debe usarse dentro de <AuthProvider>.');
  }
  return contexto;
}
