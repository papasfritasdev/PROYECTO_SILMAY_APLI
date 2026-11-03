package ec.silmay.app.presentation.sesion

import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.usecase.CerrarSesionUseCase
import ec.silmay.app.domain.usecase.ObtenerSesionActualUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val obtenerSesion: ObtenerSesionActualUseCase = mockk()
    private val cerrarSesion: CerrarSesionUseCase = mockk()

    private val sesion = UsuarioSesion(
        uid = "op-1",
        email = "operador@silmay.ec",
        nombres = "Carlos Operador",
        cedula = "0922879580",
        rol = Rol.OPERADOR,
        zonaAsignada = "cantera-banos",
        estado = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { cerrarSesion() } returns AppResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `arranca en Verificando y pasa a Autenticada si hay sesion persistida`() = runTest {
        coEvery { obtenerSesion() } returns sesion
        val viewModel = SesionViewModel(obtenerSesion, cerrarSesion)

        assertEquals(SesionUiState.Verificando, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(SesionUiState.Autenticada(sesion), viewModel.uiState.value)
    }

    @Test
    fun `sin sesion persistida pasa a SinSesion`() = runTest {
        coEvery { obtenerSesion() } returns null
        val viewModel = SesionViewModel(obtenerSesion, cerrarSesion)

        advanceUntilIdle()
        assertEquals(SesionUiState.SinSesion, viewModel.uiState.value)
    }

    @Test
    fun `CA1 - login exitoso navega al menu con la sesion del usuario`() = runTest {
        coEvery { obtenerSesion() } returns null
        val viewModel = SesionViewModel(obtenerSesion, cerrarSesion)
        advanceUntilIdle()

        viewModel.onLoginExitoso(sesion)

        assertEquals(SesionUiState.Autenticada(sesion), viewModel.uiState.value)
    }

    @Test
    fun `cerrar sesion invoca el caso de uso y vuelve a SinSesion`() = runTest {
        coEvery { obtenerSesion() } returns sesion
        val viewModel = SesionViewModel(obtenerSesion, cerrarSesion)
        advanceUntilIdle()

        viewModel.cerrarSesion()
        advanceUntilIdle()

        assertEquals(SesionUiState.SinSesion, viewModel.uiState.value)
        coVerify(exactly = 1) { cerrarSesion() }
    }
}
