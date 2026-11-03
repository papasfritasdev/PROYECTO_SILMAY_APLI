package ec.silmay.app.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Rol
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Pruebas del mapeo de errores de Firebase Auth a mensajes normalizados (HU-01a CA2, CA3, CA5)
 * y de la recuperación de sesión offline.
 */
class AuthRepositoryImplTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        auth = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        repository = AuthRepositoryImpl(auth, firestore)
    }

    private fun loginFallaCon(error: Exception) {
        every { auth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forException(error)
    }

    private fun errorDeUsuario(codigo: String): FirebaseAuthInvalidUserException {
        val error = mockk<FirebaseAuthInvalidUserException>(relaxed = true)
        every { error.errorCode } returns codigo
        return error
    }

    @Test
    fun `CA3 - cuenta deshabilitada en Auth (baja logica) retorna MSG-03`() = runTest {
        loginFallaCon(errorDeUsuario(AuthRepositoryImpl.ERROR_USER_DISABLED))

        val resultado = repository.iniciarSesion("baja@silmay.ec", "Password123")

        assertEquals(AppMessage.MSG_03, (resultado as AppResult.Error).message)
    }

    @Test
    fun `CA2 - usuario inexistente retorna el mensaje opaco MSG-01`() = runTest {
        loginFallaCon(errorDeUsuario("ERROR_USER_NOT_FOUND"))

        val resultado = repository.iniciarSesion("nadie@silmay.ec", "Password123")

        assertEquals(AppMessage.MSG_01, (resultado as AppResult.Error).message)
    }

    @Test
    fun `CA2 - contrasena incorrecta retorna el mensaje opaco MSG-01`() = runTest {
        loginFallaCon(mockk<FirebaseAuthInvalidCredentialsException>(relaxed = true))

        val resultado = repository.iniciarSesion("operador@silmay.ec", "incorrecta")

        assertEquals(AppMessage.MSG_01, (resultado as AppResult.Error).message)
    }

    @Test
    fun `CA5 - sin red en el primer login retorna MSG-02`() = runTest {
        loginFallaCon(mockk<FirebaseNetworkException>(relaxed = true))

        val resultado = repository.iniciarSesion("operador@silmay.ec", "Password123")

        assertEquals(AppMessage.MSG_02, (resultado as AppResult.Error).message)
    }

    @Test
    fun `obtenerSesionActual sin usuario autenticado retorna null`() = runTest {
        every { auth.currentUser } returns null

        assertNull(repository.obtenerSesionActual())
    }

    @Test
    fun `obtenerSesionActual offline usa el rol del perfil cacheado si el token no refresca`() = runTest {
        val user = mockk<FirebaseUser>()
        every { user.uid } returns "op-1"
        every { user.getIdToken(false) } returns Tasks.forException(RuntimeException("offline"))
        every { auth.currentUser } returns user

        val doc = mockk<DocumentSnapshot>()
        every { doc.exists() } returns true
        every { doc.id } returns "op-1"
        every { doc.getString("email") } returns "operador@silmay.ec"
        every { doc.getString("nombres") } returns "Carlos Operador"
        every { doc.getString("cedula") } returns "0922879580"
        every { doc.getString("zonaAsignada") } returns "cantera-banos"
        every { doc.getString("rol") } returns "operador"
        every { doc.getBoolean("estado") } returns true
        every {
            firestore.collection(AuthRepositoryImpl.COLECCION_USUARIOS).document("op-1").get(Source.CACHE)
        } returns Tasks.forResult(doc)

        val sesion = repository.obtenerSesionActual()

        assertEquals(Rol.OPERADOR, sesion?.rol)
        assertEquals("cantera-banos", sesion?.zonaAsignada)
    }
}
