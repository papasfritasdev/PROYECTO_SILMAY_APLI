import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from './App';
import { auth, db, functions } from './data/firebase';
import { FirebaseAuthRepository } from './data/repository/FirebaseAuthRepository';
import { FirebaseUsuariosRepository } from './data/repository/FirebaseUsuariosRepository';
import { AuthProvider } from './hooks/AuthContext';
import { RepositoriosProvider, type Repositorios } from './hooks/RepositoriosContext';
import './styles.css';

// Composición raíz: única pieza que conoce las implementaciones concretas (DI manual).
const authRepository = new FirebaseAuthRepository(auth, db);
const repositorios: Repositorios = {
  usuarios: new FirebaseUsuariosRepository(db, functions)
};

const contenedor = document.getElementById('root');
if (!contenedor) {
  throw new Error('No se encontró el elemento #root.');
}

createRoot(contenedor).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider repository={authRepository}>
        <RepositoriosProvider repositorios={repositorios}>
          <AppRoutes />
        </RepositoriosProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>
);
