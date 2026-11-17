package ec.silmay.app.domain.model

/**
 * Representa un elemento maestro de catálogo en SILMAY (docs/firebase-schema.md §2.10).
 * Colección: catalogos/{tipo}/items
 * Puro Kotlin (Clean Architecture).
 */
data class ItemCatalogo(
    val id: String,
    val nombre: String,
    val activo: Boolean = true
)
