package ec.silmay.app.data.mapper

import com.google.firebase.firestore.DocumentSnapshot
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.UsuarioSesion

/**
 * Mapeador bidireccional entre la colección 'usuarios' de Cloud Firestore y la entidad de dominio UsuarioSesion.
 */
object UsuarioMapper {

    fun desdeDocumento(doc: DocumentSnapshot, claimRole: String? = null): UsuarioSesion? {
        val uid = doc.id
        val email = doc.getString("email") ?: return null
        val nombres = doc.getString("nombres") ?: ""
        val cedula = doc.getString("cedula") ?: ""
        val estado = doc.getBoolean("estado") ?: false
        val zonaAsignada = doc.getString("zonaAsignada") ?: "cantera-banos"

        // El rol se valida prioritariamente desde el Custom Claim o en su defecto del documento
        val rolString = claimRole ?: doc.getString("rol")
        val rol = Rol.desdeValor(rolString) ?: return null

        return UsuarioSesion(
            uid = uid,
            email = email,
            nombres = nombres,
            cedula = cedula,
            rol = rol,
            zonaAsignada = zonaAsignada,
            estado = estado
        )
    }
}
