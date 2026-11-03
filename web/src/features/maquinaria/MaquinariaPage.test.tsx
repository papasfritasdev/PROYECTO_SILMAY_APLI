import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from '../../App';
import { APP_MESSAGES } from '../../domain/constants/messages';
import { AuthProvider } from '../../hooks/AuthContext';
import { RepositoriosProvider } from '../../hooks/RepositoriosContext';
import { FakeAuthRepository, sesionDePrueba } from '../../test/FakeAuthRepository';
import { FakeMaquinariaRepository, maquinaDePrueba } from '../../test/FakeMaquinariaRepository';
import { FakeUsuariosRepository } from '../../test/FakeUsuariosRepository';

function renderMaquinaria(repo: FakeMaquinariaRepository): void {
  render(
    <MemoryRouter initialEntries={['/maquinaria']}>
      <AuthProvider repository={FakeAuthRepository.conSesionPersistida(sesionDePrueba('admin'))}>
        <RepositoriosProvider repositorios={{ usuarios: new FakeUsuariosRepository(), maquinaria: repo }}>
          <AppRoutes />
        </RepositoriosProvider>
      </AuthProvider>
    </MemoryRouter>
  );
}

const escribir = (etiqueta: string, valor: string): void => {
  fireEvent.change(screen.getByLabelText(etiqueta), { target: { value: valor } });
};
const botonGuardar = (): HTMLButtonElement => screen.getByRole('button', { name: 'Guardar' });

function completarVolqueta(): void {
  fireEvent.change(screen.getByLabelText('Tipo de equipo'), { target: { value: 'volqueta' } });
  escribir('Código interno', 'vol-03');
  escribir('Placa', 'tba1234');
  escribir('Marca', 'Hino');
  escribir('Modelo', '500');
  escribir('Horómetro base', '120.5');
  escribir('Intervalo de mantenimiento', '500');
}

describe('MaquinariaPage (HU-09)', () => {
  it('lista la flota con horómetros y estado', async () => {
    renderMaquinaria(new FakeMaquinariaRepository([maquinaDePrueba(), maquinaDePrueba({ codigoInterno: 'EX-02', estado: 'inactiva' })]));

    const tabla = await screen.findByRole('table');
    expect(within(tabla).getByText('EX-01')).toBeTruthy();
    expect(within(tabla).getAllByText('1100.0 h')).toHaveLength(2);
    expect(within(tabla).getAllByText('1250.0 h')).toHaveLength(2);
    expect(within(tabla).getByText('Inactiva')).toBeTruthy();
  });

  it('CA1: muestra el próximo mantenimiento calculado y registra la unidad', async () => {
    const repo = new FakeMaquinariaRepository();
    renderMaquinaria(repo);

    fireEvent.click(await screen.findByRole('button', { name: 'Nueva unidad' }));
    expect(botonGuardar().disabled).toBe(true);
    completarVolqueta();

    expect(screen.getByText('620.5 h')).toBeTruthy();
    fireEvent.click(botonGuardar());

    await waitFor(() => expect(repo.registradas).toHaveLength(1));
    expect(repo.registradas[0]).toMatchObject({ codigoInterno: 'VOL-03', placa: 'TBA-1234', estado: 'operativa' });
    expect(await within(screen.getByRole('table')).findByText('VOL-03')).toBeTruthy();
  });

  it('una volqueta sin placa mantiene Guardar bloqueado y marca el campo', async () => {
    renderMaquinaria(new FakeMaquinariaRepository());
    fireEvent.click(await screen.findByRole('button', { name: 'Nueva unidad' }));
    completarVolqueta();
    escribir('Placa', '');

    expect(screen.getByLabelText('Placa').getAttribute('aria-invalid')).toBe('true');
    expect(botonGuardar().disabled).toBe(true);
  });

  it('CA2: código o placa duplicados muestran MSG-09', async () => {
    renderMaquinaria(new FakeMaquinariaRepository([maquinaDePrueba({ codigoInterno: 'VOL-01', placa: 'TBA-1234' })]));

    fireEvent.click(await screen.findByRole('button', { name: 'Nueva unidad' }));
    completarVolqueta();
    fireEvent.click(botonGuardar());

    expect((await screen.findByRole('alert')).textContent).toBe(APP_MESSAGES['MSG-09']);
  });

  it('en edición, código y horómetros quedan bloqueados', async () => {
    const repo = new FakeMaquinariaRepository([maquinaDePrueba()]);
    renderMaquinaria(repo);

    fireEvent.click(await screen.findByRole('button', { name: 'Editar' }));
    expect((screen.getByLabelText('Código interno') as HTMLInputElement).disabled).toBe(true);
    expect((screen.getByLabelText('Horómetro base') as HTMLInputElement).disabled).toBe(true);

    escribir('Modelo', '336');
    fireEvent.click(botonGuardar());
    await waitFor(() => expect(repo.actualizaciones).toHaveLength(1));
    expect(repo.actualizaciones[0].cambios.modelo).toBe('336');
  });

  it('baja lógica y reactivación', async () => {
    const repo = new FakeMaquinariaRepository([maquinaDePrueba()]);
    renderMaquinaria(repo);

    fireEvent.click(await screen.findByRole('button', { name: 'Dar de baja' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Reactivar' }));

    await waitFor(() => expect(repo.actualizaciones).toHaveLength(2));
    expect(repo.actualizaciones.map((a) => a.cambios.estado)).toEqual(['inactiva', 'operativa']);
  });

  it('CA3: un rechazo de permisos muestra MSG-13', async () => {
    const repo = new FakeMaquinariaRepository([maquinaDePrueba()]);
    repo.errorForzado = 'MSG-13';
    renderMaquinaria(repo);

    fireEvent.click(await screen.findByRole('button', { name: 'Dar de baja' }));
    expect((await screen.findByRole('alert')).textContent).toBe(APP_MESSAGES['MSG-13']);
  });
});
