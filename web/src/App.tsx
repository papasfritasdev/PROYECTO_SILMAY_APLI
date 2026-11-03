import { Navigate, Route, Routes } from 'react-router-dom';
import { MODULOS_WEB } from './domain/navigation/modulosPorRol';
import { LoginPage } from './features/auth/LoginPage';
import { RutaProtegida } from './features/auth/RutaProtegida';
import { InicioPage } from './features/panel/InicioPage';
import { ModuloPendiente } from './features/panel/ModuloPendiente';
import { PanelLayout } from './features/panel/PanelLayout';

/** Rutas de la plataforma web. Cada módulo exige sesión y permiso de rol (HT-08). */
export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <RutaProtegida>
            <PanelLayout />
          </RutaProtegida>
        }
      >
        <Route index element={<InicioPage />} />
        {MODULOS_WEB.map((modulo) => (
          <Route
            key={modulo}
            path={modulo}
            element={
              <RutaProtegida modulo={modulo}>
                <ModuloPendiente modulo={modulo} />
              </RutaProtegida>
            }
          />
        ))}
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
