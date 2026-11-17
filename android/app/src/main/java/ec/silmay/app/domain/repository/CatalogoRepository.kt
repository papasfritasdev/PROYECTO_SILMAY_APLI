package ec.silmay.app.domain.repository

import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ItemCatalogo
import ec.silmay.app.domain.model.Maquinaria
import ec.silmay.app.domain.model.TipoCatalogo

/**
 * Contrato de Dominio para acceso a catálogos maestros y maquinaria (HU-07).
 * Cero dependencias de Android SDK o Firebase SDK (Clean Architecture estricta).
 */
interface CatalogoRepository {
    /**
     * Obtiene los ítems de un catálogo maestro (materiales, clientes, lugares, rubros, motivos).
     * @param soloActivos Si es `true`, filtra elementos con baja lógica (`activo == true`).
     */
    suspend fun obtenerItems(tipo: TipoCatalogo, soloActivos: Boolean = true): AppResult<List<ItemCatalogo>>

    /**
     * Obtiene la lista de maquinaria y volquetas registradas.
     * @param soloOperativas Si es `true`, retorna solo aquellas en estado operativo.
     */
    suspend fun obtenerMaquinaria(soloOperativas: Boolean = true): AppResult<List<Maquinaria>>

    /**
     * Obtiene una unidad de maquinaria por su código interno único (Document ID en Firestore).
     */
    suspend fun obtenerMaquinariaPorCodigo(codigoInterno: String): AppResult<Maquinaria?>
}
