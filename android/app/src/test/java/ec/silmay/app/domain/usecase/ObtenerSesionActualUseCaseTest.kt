package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Pruebas de la recuperación de sesión persistida (HU-01a CA1, sync-protocol §5). */
class ObtenerSesionActualUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: ObtenerSesionActualUseCase

    @Before
    fun setUp() {
        authRepository = mockk(relaxed = true)
        coEvery { authRepository.cerrarSesion() } returns AppResult.Success(Unit)
        useCase = ObtenerSesionActualUseCase(authRepository)
    }

    private fun sesion(rol: Rol, estado: Boolean = true) = UsuarioSesion(
        uid = "uid-1",
        email = "usuario@silmay.ec",
        nombres = "Usuario Prueba",
        cedula = "1710034065",
        rol = rol,
        zonaAsignada = "cantera-banos",
        estado = estado
    )

    @Test
    fun `sin sesion persistida retorna null sin revocar nada`() = runTest {
        coEvery { authRepository.obtenerSesionActual() } returns null

        assertNull(useCase())
        coVerify(exactly = 0) { authRepository.cerrarSesion() }
    }

    @Test
    fun `operador activo recupera su sesion desde disco`() = runTest {
        val esperada = sesion(Rol.OPERADOR)
        coEvery { authRepository.obtenerSesionActual() } returns esperada

        assertEquals(esperada, useCase())
        coVerify(exactly = 0) { authRepository.cerrarSesion() }
    }

    @Test
    fun `supervisor activo recupera su sesion desde disco`() = runTest {
        val esperada = sesion(Rol.SUPERVISOR)
        coEvery { authRepository.obtenerSesionActual() } returns esperada

        assertEquals(esperada, useCase())
    }

    @Test
    fun `cuenta dada de baja se revoca al reabrir la app`() = runTest {
        coEvery { authRepository.obtenerSesionActual() } returns sesion(Rol.OPERADOR, estado = false)

        assertNull(useCase())
        coVerify(exactly = 1) { authRepository.cerrarSesion() }
    }

    @Test
    fun `roles web persistidos se revocan en movil`() = runTest {
        coEvery { authRepository.obtenerSesionActual() } returns sesion(Rol.ADMIN)
        assertNull(useCase())

        coEvery { authRepository.obtenerSesionActual() } returns sesion(Rol.CONTADOR)
        assertNull(useCase())

        coVerify(exactly = 2) { authRepository.cerrarSesion() }
    }
}
