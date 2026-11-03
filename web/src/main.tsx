import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from './App';
import { auth, db } from './data/firebase';
import { FirebaseAuthRepository } from './data/repository/FirebaseAuthRepository';
import { AuthProvider } from './hooks/AuthContext';
import './styles.css';

// Composición raíz: única pieza que conoce las implementaciones concretas (DI manual).
const authRepository = new FirebaseAuthRepository(auth, db);

const contenedor = document.getElementById('root');
if (!contenedor) {
  throw new Error('No se encontró el elemento #root.');
}

createRoot(contenedor).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider repository={authRepository}>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>
);
