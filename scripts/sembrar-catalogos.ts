/**
 * Siembra inicial de catálogos maestros y parámetros del Sistema SILMAY (HT-05, Sprint 2).
 * Fuentes: docs/firebase-schema.md §2.10–2.11, docs/specs/sprint-03-specs.md (HU-03),
 *          docs/business-formulas.md §1.
 *
 * Idempotente: usa `doc(id).create()`; los documentos existentes NO se sobrescriben,
 * preservando las bajas lógicas o ediciones que el Administrador haya hecho desde la web.
 *
 * Uso:
 *   Emulador local: npm run sembrar-catalogos:local   (incluye datos de ejemplo)
 *   Nube:           npm run sembrar-catalogos:prod    (solo catálogos canónicos)
 */
import { getApps, initializeApp } from 'firebase-admin/app';
import { DocumentReference, getFirestore } from 'firebase-admin/firestore';

type TipoCatalogo = 'materiales' | 'clientes' | 'lugares' | 'rubros' | 'motivos';

interface ItemCatalogo {
  id: string;
  nombre: string;
  activo: boolean;
}

/** Código gRPC ALREADY_EXISTS devuelto por create() sobre un documento existente. */
const GRPC_ALREADY_EXISTS = 6;

export function slug(nombre: string): string {
  return nombre
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

function items(nombres: string[]): ItemCatalogo[] {
  return nombres.map((nombre) => ({ id: slug(nombre), nombre, activo: true }));
}

/** Catálogos canónicos definidos en la documentación del proyecto. */
const CATALOGOS_CANONICOS: Record<'materiales' | 'rubros' | 'motivos', ItemCatalogo[]> = {
  // HU-03 CA1
  materiales: items(['Arena normal', 'Arena fina', 'Piedra', 'Ripio']),
  // firebase-schema §2.4 (enum gastos.rubro)
  rubros: items(['Combustible', 'Viáticos', 'Repuestos', 'Otros']),
  // business-formulas §1.1 y firebase-schema §2.2 (deducciones.esFalla)
  motivos: items([
    'Almuerzo',
    'Traslado',
    'Clima',
    'Falla mecánica',
    'Falla hidráulica',
    'Neumáticos'
  ])
};

/**
 * Datos de EJEMPLO solo para el emulador: la documentación no define los frentes ni
 * clientes reales. Reemplazar por los datos oficiales de SILMAY antes del piloto.
 */
const CATALOGOS_DEMO: Record<'clientes' | 'lugares', ItemCatalogo[]> = {
  lugares: items(['Frente 1', 'Frente 2', 'Rio Pastaza']),
  clientes: items(['Cliente Contado', 'Constructora Demo'])
};

const PARAMETROS_SISTEMA = {
  umbralProximoMantenimiento: 0.9,
  zonaHoraria: 'America/Guayaquil'
};

function esYaExistente(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    (error as { code: unknown }).code === GRPC_ALREADY_EXISTS
  );
}

async function crearSiNoExiste(ref: DocumentReference, data: object): Promise<boolean> {
  try {
    await ref.create(data);
    return true;
  } catch (error: unknown) {
    if (esYaExistente(error)) return false;
    throw error;
  }
}

async function main(): Promise<void> {
  const isLocal = process.argv.includes('--local') || !process.env.GOOGLE_APPLICATION_CREDENTIALS;

  if (isLocal) {
    process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080';
    console.log(` Conectando a Firestore Emulator: ${process.env.FIRESTORE_EMULATOR_HOST}`);
  } else {
    console.log(' Conectando a Firebase Cloud...');
  }

  if (getApps().length === 0) {
    initializeApp({ projectId: process.env.FIREBASE_PROJECT_ID || 'silmay-dev' });
  }
  const db = getFirestore();

  const catalogos: Partial<Record<TipoCatalogo, ItemCatalogo[]>> = {
    ...CATALOGOS_CANONICOS,
    ...(isLocal ? CATALOGOS_DEMO : {})
  };

  let creados = 0;
  let existentes = 0;

  for (const [tipo, lista] of Object.entries(catalogos)) {
    for (const item of lista ?? []) {
      const ref = db.collection('catalogos').doc(tipo).collection('items').doc(item.id);
      if (await crearSiNoExiste(ref, item)) {
        creados++;
      } else {
        existentes++;
      }
    }
    console.log(`   catalogos/${tipo}/items: ${(lista ?? []).length} ítems verificados`);
  }

  const paramRef = db.collection('parametros').doc('sistema');
  if (await crearSiNoExiste(paramRef, PARAMETROS_SISTEMA)) {
    creados++;
  } else {
    existentes++;
  }
  console.log('   parametros/sistema verificado');

  console.log(`\n Siembra completada: ${creados} creados, ${existentes} ya existían (sin cambios).`);
}

main().catch((error: unknown) => {
  console.error('\n❌ Error al sembrar catálogos:', error);
  process.exit(1);
});
