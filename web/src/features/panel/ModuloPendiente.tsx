import type { ModuloWeb } from '../../domain/navigation/modulosPorRol';
import { ETIQUETA_MODULO } from './etiquetas';

/** Marcador de módulos que se implementan en historias posteriores. */
export function ModuloPendiente({ modulo }: { readonly modulo: ModuloWeb }) {
  return (
    <section>
      <h2>{ETIQUETA_MODULO[modulo]}</h2>
      <p>Disponible en próximos sprints.</p>
    </section>
  );
}
