import type { ModuloWeb } from '../../domain/navigation/modulosPorRol';
import type { Rol } from '../../domain/model/Rol';

/** Etiquetas de interfaz (no son mensajes de validación: no pertenecen al catálogo MSG-XX). */
export const ETIQUETA_MODULO: Readonly<Record<ModuloWeb, string>> = {
  monitoreo: 'Monitoreo operativo',
  validacion: 'Validación contable',
  reporteria: 'Reportería',
  alertas: 'Alertas',
  usuarios: 'Usuarios',
  maquinaria: 'Maquinaria y volquetas',
  catalogos: 'Catálogos y parámetros'
};

export const ETIQUETA_ROL: Readonly<Record<Rol, string>> = {
  operador: 'Operador',
  supervisor: 'Supervisor',
  contador: 'Contador',
  admin: 'Administrador'
};
