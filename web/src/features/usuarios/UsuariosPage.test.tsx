import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from '../../App';
import { APP_MESSAGES } from '../../domain/constants/messages';
import { AuthProvider } from '../../hooks/AuthContext';
import { RepositoriosProvider } from '../../hooks/RepositoriosContext';
import { FakeAuthRepository, sesionDePrueba } from '../../test/FakeAuthRepository';
import { FakeMaquinariaRepository } from '../../test/FakeMaquinariaRepository';
import { FakeUsuariosRepository, usuarioDePrueba } from '../../test/FakeUsuariosRepository';

const ADMIN = sesionDePrueba('admin');

function renderUsuarios(repo: FakeUsuariosRepository): void {
  render(
    <MemoryRouter initialEntries={['/usuarios']}>
      <AuthProvider repository={FakeAuthRepository.conSesionPersistida(ADMIN)}>
        <RepositoriosProvider repositorios={{ usuarios: repo, maquinaria: new FakeMaquinariaRepository() }}>
          <AppRoutes />
        </RepositoriosProvider>
      </AuthProvider>
    </MemoryRouter>
  );
}

function escribir(etiqueta: string, valor: string): void {
  fireEvent.change(screen.getByLabelText(etiqueta), { target: { value: valor } });
}

function completarAlta(): void {
  escribir('Nombres', 'Juan Pérez');
  escribir('Cédula', '1710034065');
  escribir('Teléfono', '0991234567');
  escribir('Correo electrónico', 'juan@silmay.ec');
  escribir('Contraseña', 'Silmay2026');
  escribir('Zona asignada', 'cantera-banos');
  escribir('Cargo', 'Operador de Excavadora');
}

const botonGuardar = (): HTMLButtonElement => screen.getByRole('button', { name: 'Guardar' });

describe('UsuariosPage (HU-02 web)', () => {
  it('lista los usuarios existentes con su rol y estado', async () => {
    renderUsuarios(new FakeUsuariosRepository([usuarioDePrueba(), usuarioDePrueba({ uid: 'u2', nombres: 'Ana Baja', estado: false })]));

    const tabla = await screen.findByRole('table');
    expect(within(tabla).getByText('Carlos Operador')).toBeTruthy();
    expect(within(tabla).getByText('Inactivo')).toBeTruthy();
    expect(within(tabla).getAllByText('Operador').length).toBeGreaterThan(0);
  });

  it('CA1: crea un usuario; Guardar solo se habilita con el formulario válido', async () => {
    const repo = new FakeUsuariosRepository();
    renderUsuarios(repo);

    fireEvent.click(await screen.findByRole('button', { name: 'Nuevo usuario' }));
    expect(botonGuardar().disabled).toBe(true);

    completarAlta();
    expect(botonGuardar().disabled).toBe(false);
    fireEvent.click(botonGuardar());

    await waitFor(() => expect(repo.guardados).toHaveLength(1));
    expect(repo.guardados[0]).toMatchObject({ email: 'juan@silmay.ec', rol: 'operador', estado: true });
    expect(await screen.findByText('Juan Pérez')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Guardar' })).toBeNull();
  });

  it('marca el campo inválido con su ayuda de formato y mantiene Guardar bloqueado', async () => {
    renderUsuarios(new FakeUsuariosRepository());
    fireEvent.click(await screen.findByRole('button', { name: 'Nuevo usuario' }));

    completarAlta();
    escribir('Cédula', '1234567890');

    expect(screen.getByLabelText('Cédula').getAttribute('aria-invalid')).toBe('true');
    expect(screen.getByText('Cédula ecuatoriana · 10 dígitos')).toBeTruthy();
    expect(botonGuardar().disabled).toBe(true);
  });

  it('CA2: muestra MSG-17 si el correo o la cédula ya existen', async () => {
    const repo = new FakeUsuariosRepository();
    repo.errorAlGuardar = 'MSG-17';
    renderUsuarios(repo);

    fireEvent.click(await screen.findByRole('button', { name: 'Nuevo usuario' }));
    completarAlta();
    fireEvent.click(botonGuardar());

    expect((await screen.findByRole('alert')).textContent).toBe(APP_MESSAGES['MSG-17']);
    expect(botonGuardar()).toBeTruthy();
  });

  it('CA3: da de baja y reactiva a un usuario', async () => {
    const repo = new FakeUsuariosRepository([usuarioDePrueba()]);
    renderUsuarios(repo);

    fireEvent.click(await screen.findByRole('button', { name: 'Dar de baja' }));
    expect(await screen.findByRole('button', { name: 'Reactivar' })).toBeTruthy();
    expect(repo.guardados[0]).toMatchObject({ uid: 'uid-op', estado: false });

    fireEvent.click(screen.getByRole('button', { name: 'Reactivar' }));
    expect(await screen.findByRole('button', { name: 'Dar de baja' })).toBeTruthy();
    expect(repo.guardados[1]).toMatchObject({ uid: 'uid-op', estado: true });
  });

  it('el admin no puede darse de baja ni cambiar su propio rol', async () => {
    const propio = usuarioDePrueba({ uid: ADMIN.uid, nombres: 'Usuario admin', rol: 'admin' });
    renderUsuarios(new FakeUsuariosRepository([propio]));

    const tabla = await screen.findByRole('table');
    const fila = within(tabla).getByText('Usuario admin').closest('tr') as HTMLElement;
    expect((within(fila).getByRole('button', { name: 'Dar de baja' }) as HTMLButtonElement).disabled).toBe(true);

    fireEvent.click(within(fila).getByRole('button', { name: 'Editar' }));
    expect((screen.getByLabelText('Rol') as HTMLSelectElement).disabled).toBe(true);
  });

  it('al editar, la contraseña vacía no se envía y el resto del perfil se conserva', async () => {
    const repo = new FakeUsuariosRepository([usuarioDePrueba()]);
    renderUsuarios(repo);

    fireEvent.click(await screen.findByRole('button', { name: 'Editar' }));
    escribir('Cargo', 'Jefe de Frente');
    fireEvent.click(botonGuardar());

    await waitFor(() => expect(repo.guardados).toHaveLength(1));
    expect(repo.guardados[0]).toMatchObject({ uid: 'uid-op', cargo: 'Jefe de Frente', cedula: '0922879580' });
    expect(repo.guardados[0].password).toBeUndefined();
  });

  it('un error inesperado al dar de baja muestra MSG-18', async () => {
    const repo = new FakeUsuariosRepository([usuarioDePrueba()]);
    repo.errorAlGuardar = 'MSG-18';
    renderUsuarios(repo);

    fireEvent.click(await screen.findByRole('button', { name: 'Dar de baja' }));
    expect((await screen.findByRole('alert')).textContent).toBe(APP_MESSAGES['MSG-18']);
  });
});
