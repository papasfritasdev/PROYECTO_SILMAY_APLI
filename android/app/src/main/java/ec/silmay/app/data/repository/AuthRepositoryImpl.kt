package ec.silmay.app.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
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

    companion object {
        const val COLECCION_USUARIOS = "usuarios"

        /** Código que Firebase Auth devuelve para cuentas con `disabled: true` (baja lógica). */
        const val ERROR_USER_DISABLED = "ERROR_USER_DISABLED"
    }

    override suspend fun iniciarSesion(email: String, contrasena: String): AppResult<UsuarioSesion> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, contrasena).await()
            val firebaseUser = authResult.user ?: return AppResult.Error(AppMessage.MSG_01)

            // Obtener Custom Claims para validación autoritativa de rol
            val claimRole = leerClaimRol(firebaseUser)

            // Consultar perfil de usuario (servidor; si la red falla, caché persistente local)
            val doc = try {
                firestore.collection(COLECCION_USUARIOS)
                    .document(firebaseUser.uid)
                    .get()
                    .await()
            } catch (e: Exception) {
                firestore.collection(COLECCION_USUARIOS)
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
        } catch (e: FirebaseAuthInvalidUserException) {
            // CA3: la baja lógica de gestionarUsuario deshabilita la cuenta en Auth.
            // El resto de errores de usuario (p. ej. inexistente) siguen siendo opacos (CA2).
            if (e.errorCode == ERROR_USER_DISABLED) {
                AppResult.Error(AppMessage.MSG_03)
            } else {
                AppResult.Error(AppMessage.MSG_01)
            }
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

    /**
     * Recupera la sesión persistida exclusivamente desde disco (Source.CACHE), sin red.
     * Si el token no puede refrescarse offline, el rol se toma del perfil cacheado,
     * que fue escrito por gestionarUsuario junto con el Custom Claim.
     */
    override suspend fun obtenerSesionActual(): UsuarioSesion? {
        val currentUser = auth.currentUser ?: return null
        return try {
            val doc = firestore.collection(COLECCION_USUARIOS)
                .document(currentUser.uid)
                .get(Source.CACHE)
                .await()

            if (doc.exists()) {
                UsuarioMapper.desdeDocumento(doc, leerClaimRol(currentUser))
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun leerClaimRol(user: FirebaseUser): String? {
        return try {
            user.getIdToken(false).await().claims["role"] as? String
        } catch (_: Exception) {
            null
        }
    }
}
