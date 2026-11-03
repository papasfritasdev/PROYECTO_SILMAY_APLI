package ec.silmay.app.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import ec.silmay.app.data.mapper.UsuarioMapper
import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun iniciarSesion(email: String, contrasena: String): AppResult<UsuarioSesion> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, contrasena).await()
            val firebaseUser = authResult.user ?: return AppResult.Error(AppMessage.MSG_01)

            // Obtener Custom Claims para validación autoritativa de rol
            val tokenResult = firebaseUser.getIdToken(false).await()
            val claimRole = tokenResult.claims["role"] as? String

            // Consultar perfil de usuario (intentar primero con caché/servidor según disponibilidad)
            val doc = try {
                firestore.collection("usuarios")
                    .document(firebaseUser.uid)
                    .get()
                    .await()
            } catch (e: Exception) {
                // Si la red falla, intentar leer desde la caché persistente local
                firestore.collection("usuarios")
                    .document(firebaseUser.uid)
                    .get(Source.CACHE)
                    .await()
            }

            if (!doc.exists()) {
                return AppResult.Error(AppMessage.MSG_01)
            }

            val sesion = UsuarioMapper.desdeDocumento(doc, claimRole)
                ?: return AppResult.Error(AppMessage.MSG_01)

            AppResult.Success(sesion)
        } catch (_: FirebaseNetworkException) {
            // CA5: Primer inicio de sesión sin red
            AppResult.Error(AppMessage.MSG_02)
        } catch (_: FirebaseAuthException) {
            // CA2: Mensaje opaco ante credenciales no válidas
            AppResult.Error(AppMessage.MSG_01)
        } catch (e: Exception) {
            AppResult.Error(AppMessage.MSG_01, e)
        }
    }

    override suspend fun cerrarSesion(): AppResult<Unit> {
        return try {
            auth.signOut()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppMessage.MSG_01, e)
        }
    }

    override suspend fun obtenerSesionActual(): UsuarioSesion? {
        val currentUser = auth.currentUser ?: return null
        return try {
            val doc = firestore.collection("usuarios")
                .document(currentUser.uid)
                .get(Source.CACHE)
                .await()

            if (doc.exists()) {
                val token = currentUser.getIdToken(false).await()
                val claimRole = token.claims["role"] as? String
                UsuarioMapper.desdeDocumento(doc, claimRole)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
