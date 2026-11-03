import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from './App';
import { APP_MESSAGES } from './domain/constants/messages';
import type { OpcionesInactividad } from './hooks/useInactividad';
import { AuthProvider } from './hooks/AuthContext';
import { RepositoriosProvider, type Repositorios } from './hooks/RepositoriosContext';
import { FakeMaquinariaRepository } from './test/FakeMaquinariaRepository';
import { FakeUsuariosRepository } from './test/FakeUsuariosRepository';
import { FakeAuthRepository, sesionDePrueba } from './test/FakeAuthRepository';

const repositoriosVacios = (): Repositorios => ({
  usuarios: new FakeUsuariosRepository(),
  maquinaria: new FakeMaquinariaRepository()
});

function renderApp(
  repo: FakeAuthRepository,
  ruta = '/',
  inactividad?: OpcionesInactividad
): void {
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <AuthProvider repository={repo} inactividad={inactividad}>
        <RepositoriosProvider repositorios={repositoriosVacios()}>
          <AppRoutes />
        </RepositoriosProvider>
      </AuthProvider>
    </MemoryRouter>
  );
}

async function enviarLogin(email: string, contrasena = 'Silmay2026'): Promise<void> {
  fireEvent.change(await screen.findByLabelText('Correo electrónico'), { target: { value: email } });
  fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: contrasena } });
  fireEvent.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));
}

describe('Plataforma web — login y rutas protegidas (HT-08, HU-01b)', () => {
  it('sin sesión, cualquier ruta protegida redirige al login', async () => {
    renderApp(new FakeAuthRepository(), '/usuarios');
    expect(await screen.findByRole('button', { name: 'Iniciar Sesión' })).toBeTruthy();
  });

  it('CA1: el admin accede al panel con sus módulos autorizados', async () => {
    renderApp(FakeAuthRepository.conLogin(sesionDePrueba('admin')), '/login');
    await enviarLogin('admin@silmay.ec');

    expect(await screen.findByText('Bienvenido, Usuario admin')).toBeTruthy();
    const menu = screen.getByRole('navigation', { name: 'Módulos' });
    expect(menu.textContent).toContain('Usuarios');
    expect(menu.textContent).toContain('Maquinaria y volquetas');
    expect(menu.textContent).not.toContain('Validación contable');
  });

  it('CA1: el contador ve solo sus módulos', async () => {
    renderApp(FakeAuthRepository.conLogin(sesionDePrueba('contador')), '/login');
    await enviarLogin('contador@silmay.ec');

    const menu = await screen.findByRole('navigation', { name: 'Módulos' });
    expect(menu.textContent).toContain('Validación contable');
    expect(menu.textContent).not.toContain('Usuarios');
    expect(menu.textContent).not.toContain('Maquinaria y volquetas');
  });

  it('CA2: operador y supervisor ven MSG-13 y su sesión se cierra', async () => {
    for (const rol of ['operador', 'supervisor'] as const) {
      const repo = FakeAuthRepository.conLogin(sesionDePrueba(rol));
      const { unmount } = render(
        <MemoryRouter initialEntries={['/login']}>
          <AuthProvider repository={repo}>
            <RepositoriosProvider repositorios={repositoriosVacios()}>
              <AppRoutes />
            </RepositoriosProvider>
          </AuthProvider>
        </MemoryRouter>
      );
      await enviarLogin(`${rol}@silmay.ec`);

      expect((await screen.findByRole('alert')).textContent).toBe(APP_MESSAGES['MSG-13']);
      expect(repo.cierresDeSesion).toBe(1);
      unmount();
    }
  });

  it('credenciales inválidas muestran MSG-01 (opaco)', async () => {
    renderApp(FakeAuthRepository.conError('MSG-01'), '/login');
    await enviarLogin('admin@silmay.ec', 'incorrecta');
    expect((await screen.findByRole('alert')).textContent).toBe(APP_MESSAGES['MSG-01']);
  });

  it('cuenta con estado false muestra MSG-03', async () => {
    renderApp(FakeAuthRepository.conLogin(sesionDePrueba('admin', false)), '/login');
    await enviarLogin('admin@silmay.ec');
    expect((await screen.findByRole('alert')).textContent).toBe(APP_MESSAGES['MSG-03']);
  });

  it('el contador no puede entrar por URL a un módulo del admin', async () => {
    renderApp(FakeAuthRepository.conSesionPersistida(sesionDePrueba('contador')), '/usuarios');
    expect(await screen.findByText('Bienvenido, Usuario contador')).toBeTruthy();
    expect(screen.queryByRole('heading', { name: 'Usuarios' })).toBeNull();
  });

  it('el admin abre un módulo autorizado', async () => {
    renderApp(FakeAuthRepository.conSesionPersistida(sesionDePrueba('admin')), '/maquinaria');
    expect(await screen.findByRole('heading', { name: 'Maquinaria y volquetas' })).toBeTruthy();
  });

  it('restaura la sesión persistida al recargar', async () => {
    renderApp(FakeAuthRepository.conSesionPersistida(sesionDePrueba('admin')), '/');
    expect(await screen.findByText('Bienvenido, Usuario admin')).toBeTruthy();
  });

  it('cerrar sesión vuelve al login', async () => {
    const repo = FakeAuthRepository.conSesionPersistida(sesionDePrueba('admin'));
    renderApp(repo, '/');
    fireEvent.click(await screen.findByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('button', { name: 'Iniciar Sesión' })).toBeTruthy();
    expect(repo.cierresDeSesion).toBe(1);
  });

  it('CA3: la inactividad cierra la sesión y redirige al login', async () => {
    const repo = FakeAuthRepository.conSesionPersistida(sesionDePrueba('admin'));
    // Límite reducido solo para la prueba; el valor real (30 min) se prueba en useInactividad.
    renderApp(repo, '/', { limiteMs: 150, intervaloVerificacionMs: 25 });

    expect(await screen.findByText('Bienvenido, Usuario admin')).toBeTruthy();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Iniciar Sesión' })).toBeTruthy());
    expect(repo.cierresDeSesion).toBe(1);
  });
});
