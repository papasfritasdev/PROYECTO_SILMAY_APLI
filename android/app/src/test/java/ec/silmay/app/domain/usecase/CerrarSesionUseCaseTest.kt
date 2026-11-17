package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.SincronizacionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CerrarSesionUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val sincronizacionRepository: SincronizacionRepository = mockk()
    private val useCase = CerrarSesionUseCase(authRepository, sincronizacionRepository)

    private val sesionValida = UsuarioSesion(
        uid = "usr_123",
        email = "operador@silmay.ec",
        nombres = "Juan Perez",
        cedula = "0922879580",
        rol = Rol.OPERADOR,
        zonaAsignada = "cantera-banos",
        estado = true
    )

    @Test
    fun `cuando no hay pendientes cierra la sesion exitosamente`() = runTest {
        coEvery { authRepository.obtenerSesionActual() } returns sesionValida
        coEvery { sincronizacionRepository.contarPendientes(sesionValida.uid) } returns 0
        coEvery { authRepository.cerrarSesion() } returns AppResult.Success(Unit)

        val resultado = useCase()

        assertEquals(CerrarSesionResultado.Exito, resultado)
        coVerify(exactly = 1) { authRepository.cerrarSesion() }
    }

    @Test
    fun `cuando existen pendientes en cola bloquea el cierre y preserva los datos`() = runTest {
        coEvery { authRepository.obtenerSesionActual() } returns sesionValida
        coEvery { sincronizacionRepository.contarPendientes(sesionValida.uid) } returns 3

        val resultado = useCase()

        assertTrue(resultado is CerrarSesionResultado.BloqueadoPorPendientes)
        assertEquals(3, (resultado as CerrarSesionResultado.BloqueadoPorPendientes).cantidadPendientes)
        coVerify(exactly = 0) { authRepository.cerrarSesion() }
    }

    @Test
    fun `cuando no hay sesion activa delega el cierre sin consultar pendientes`() = runTest {
        coEvery { authRepository.obtenerSesionActual() } returns null
        coEvery { authRepository.cerrarSesion() } returns AppResult.Success(Unit)

        val resultado = useCase()

        assertEquals(CerrarSesionResultado.Exito, resultado)
        coVerify(exactly = 0) { sincronizacionRepository.contarPendientes(any()) }
        coVerify(exactly = 1) { authRepository.cerrarSesion() }
    }

    @Test
    fun `cuando falla el cierre en repositorio propaga el error correspondiente`() = runTest {
        coEvery { authRepository.obtenerSesionActual() } returns sesionValida
        coEvery { sincronizacionRepository.contarPendientes(sesionValida.uid) } returns 0
        coEvery { authRepository.cerrarSesion() } returns AppResult.Error(AppMessage.MSG_18)

        val resultado = useCase()

        assertTrue(resultado is CerrarSesionResultado.Error)
        assertEquals(AppMessage.MSG_18, (resultado as CerrarSesionResultado.Error).message)
    }
}
