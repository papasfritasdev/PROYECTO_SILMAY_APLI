import { esRol } from '../model/Rol';
import type { DatosUsuario } from '../model/Usuario';
import {
  esCedulaEcuatorianaValida,
  esContrasenaValida,
  esEmailValido,
  esTelefonoEcuatorianoValido
} from './identidad';

export type CampoUsuario =
  | 'nombres'
  | 'cedula'
  | 'telefono'
  | 'email'
  | 'password'
  | 'rol'
  | 'zonaAsignada'
  | 'cargo'
  | 'estado';

const noVacio = (valor: string): boolean => valor.trim().length > 0;

/**
 * Devuelve los campos inválidos del formulario de usuario (HU-02 CA1).
 * La contraseña es obligatoria al crear y opcional al editar.
 * No produce textos: la UI marca los campos y muestra su formato esperado.
 */
export function validarUsuario(datos: DatosUsuario): CampoUsuario[] {
  const esAlta = datos.uid === undefined;
  const invalidos: CampoUsuario[] = [];

  if (!noVacio(datos.nombres)) invalidos.push('nombres');
  if (!esCedulaEcuatorianaValida(datos.cedula.trim())) invalidos.push('cedula');
  if (!esTelefonoEcuatorianoValido(datos.telefono)) invalidos.push('telefono');
  if (!esEmailValido(datos.email.trim())) invalidos.push('email');
  if (!esRol(datos.rol)) invalidos.push('rol');
  if (!noVacio(datos.zonaAsignada)) invalidos.push('zonaAsignada');
  if (!noVacio(datos.cargo)) invalidos.push('cargo');

  const password = datos.password ?? '';
  if ((esAlta || password.length > 0) && !esContrasenaValida(password)) {
    invalidos.push('password');
  }
  return invalidos;
}

/** Normaliza el payload igual que el servidor (trim, email en minúsculas, teléfono sin separadores). */
export function normalizarUsuario(datos: DatosUsuario): DatosUsuario {
  const tipoLicencia = datos.tipoLicencia?.trim() ?? '';
  const password = datos.password ?? '';
  return {
    ...datos,
    nombres: datos.nombres.trim(),
    cedula: datos.cedula.trim(),
    telefono: datos.telefono.replace(/[\s-]/g, ''),
    email: datos.email.trim().toLowerCase(),
    zonaAsignada: datos.zonaAsignada.trim(),
    cargo: datos.cargo.trim(),
    tipoLicencia: tipoLicencia.length > 0 ? tipoLicencia : null,
    password: password.length > 0 ? password : undefined
  };
}
