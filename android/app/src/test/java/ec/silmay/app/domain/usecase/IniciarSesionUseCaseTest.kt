package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas Unitarias para IniciarSesionUseCase (Criterios de Aceptación HU-01a).
 * Cobertura de Dominio >= 70%.
 */
class IniciarSesionUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: IniciarSesionUseCase

    @Before
    fun setUp() {
        authRepository = mockk(relaxed = true)
        useCase = IniciarSesionUseCase(authRepository)
    }

    private fun crearSesionPrueba(rol: Rol, estado: Boolean = true): UsuarioSesion {
        return UsuarioSesion(
            uid = "test-uid-123",
            email = "operador@silmay.ec",
            nombres = "Juan Pérez",
            cedula = "0102030405",
            rol = rol,
            zonaAsignada = "cantera-banos",
            estado = estado
        )
    }

    @Test
    fun `cuando correo o contrasena estan vacios retorna MSG-01`() = runTest {
        val resultado1 = useCase("", "password123")
        assertTrue(resultado1 is AppResult.Error)
        assertEquals(AppMessage.MSG_01, (resultado1 as AppResult.Error).message)

        val resultado2 = useCase("usuario@silmay.ec", "   ")
        assertTrue(resultado2 is AppResult.Error)
        assertEquals(AppMessage.MSG_01, (resultado2 as AppResult.Error).message)

        coVerify(exactly = 0) { authRepository.iniciarSesion(any(), any()) }
    }

    @Test
    fun `CA2 - cuando credenciales son incorrectas retorna MSG-01`() = runTest {
        coEvery { authRepository.iniciarSesion("test@silmay.ec", "wrong") } returns
                AppResult.Error(AppMessage.MSG_01)

        val resultado = useCase("test@silmay.ec", "wrong")

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_01, (resultado as AppResult.Error).message)
    }

    @Test
    fun `CA3 - cuando cuenta esta deshabilitada estado false revoca sesion y retorna MSG-03`() = runTest {
        val sesionInactiva = crearSesionPrueba(rol = Rol.OPERADOR, estado = false)
        coEvery { authRepository.iniciarSesion("inactivo@silmay.ec", "pass123") } returns
                AppResult.Success(sesionInactiva)

        val resultado = useCase("inactivo@silmay.ec", "pass123")

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_03, (resultado as AppResult.Error).message)
        coVerify(exactly = 1) { authRepository.cerrarSesion() }
    }

    @Test
    fun `CA4 - cuando rol es admin o contador en app movil revoca sesion y retorna MSG-13`() = runTest {
        val sesionAdmin = crearSesionPrueba(rol = Rol.ADMIN, estado = true)
        coEvery { authRepository.iniciarSesion("admin@silmay.ec", "pass123") } returns
                AppResult.Success(sesionAdmin)

        val resultadoAdmin = useCase("admin@silmay.ec", "pass123")

        assertTrue(resultadoAdmin is AppResult.Error)
        assertEquals(AppMessage.MSG_13, (resultadoAdmin as AppResult.Error).message)
        coVerify(exactly = 1) { authRepository.cerrarSesion() }

        val sesionContador = crearSesionPrueba(rol = Rol.CONTADOR, estado = true)
        coEvery { authRepository.iniciarSesion("contador@silmay.ec", "pass123") } returns
                AppResult.Success(sesionContador)

        val resultadoContador = useCase("contador@silmay.ec", "pass123")

        assertTrue(resultadoContador is AppResult.Error)
        assertEquals(AppMessage.MSG_13, (resultadoContador as AppResult.Error).message)
        coVerify(exactly = 2) { authRepository.cerrarSesion() }
    }

    @Test
    fun `CA1 - cuando credenciales y rol operador son validos retorna Success`() = runTest {
        val sesionOperador = crearSesionPrueba(rol = Rol.OPERADOR, estado = true)
        coEvery { authRepository.iniciarSesion("operador@silmay.ec", "pass123") } returns
                AppResult.Success(sesionOperador)

        val resultado = useCase("operador@silmay.ec", "pass123")

        assertTrue(resultado is AppResult.Success)
        assertEquals(sesionOperador, (resultado as AppResult.Success).data)
        coVerify(exactly = 0) { authRepository.cerrarSesion() }
    }

    @Test
    fun `CA1 - cuando credenciales y rol supervisor son validos retorna Success`() = runTest {
        val sesionSupervisor = crearSesionPrueba(rol = Rol.SUPERVISOR, estado = true)
        coEvery { authRepository.iniciarSesion("supervisor@silmay.ec", "pass123") } returns
                AppResult.Success(sesionSupervisor)

        val resultado = useCase("supervisor@silmay.ec", "pass123")

        assertTrue(resultado is AppResult.Success)
        assertEquals(sesionSupervisor, (resultado as AppResult.Success).data)
        coVerify(exactly = 0) { authRepository.cerrarSesion() }
    }

    @Test
    fun `CA5 - cuando ocurre error de red en primer login retorna MSG-02`() = runTest {
        coEvery { authRepository.iniciarSesion("offline@silmay.ec", "pass123") } returns
                AppResult.Error(AppMessage.MSG_02)

        val resultado = useCase("offline@silmay.ec", "pass123")

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_02, (resultado as AppResult.Error).message)
    }
}
