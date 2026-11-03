package ec.silmay.app.presentation.login

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.usecase.IniciarSesionUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var useCase: IniciarSesionUseCase
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
        viewModel = LoginViewModel(useCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial es vacio sin errores ni sesion activa`() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.contrasena)
        assertFalse(state.isLoading)
        assertNull(state.mensajeError)
        assertNull(state.sesionActiva)
        assertFalse(state.puedeIniciarSesion)
    }

    @Test
    fun `onEmailChanged actualiza el correo y limpia el error previo`() {
        viewModel.onEmailChanged("operador@silmay.ec")
        assertEquals("operador@silmay.ec", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.mensajeError)
    }

    @Test
    fun `onContrasenaChanged actualiza la contrasena y limpia el error previo`() {
        viewModel.onContrasenaChanged("password123")
        assertEquals("password123", viewModel.uiState.value.contrasena)
        assertNull(viewModel.uiState.value.mensajeError)
    }

    @Test
    fun `iniciarSesion con credenciales correctas actualiza sesionActiva`() = runTest {
        val sesion = UsuarioSesion(
            uid = "op-123",
            email = "operador@silmay.ec",
            nombres = "Juan Pérez",
            cedula = "0102030405",
            rol = Rol.OPERADOR,
            zonaAsignada = "cantera-banos",
            estado = true
        )

        coEvery { useCase("operador@silmay.ec", "secret123") } returns AppResult.Success(sesion)

        viewModel.onEmailChanged("operador@silmay.ec")
        viewModel.onContrasenaChanged("secret123")
        assertTrue(viewModel.uiState.value.puedeIniciarSesion)

        viewModel.iniciarSesion()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(sesion, state.sesionActiva)
        assertNull(state.mensajeError)
    }

    @Test
    fun `iniciarSesion con error propaga el mensaje normalizado MSG-01`() = runTest {
        coEvery { useCase("error@silmay.ec", "wrong") } returns AppResult.Error(AppMessage.MSG_01)

        viewModel.onEmailChanged("error@silmay.ec")
        viewModel.onContrasenaChanged("wrong")

        viewModel.iniciarSesion()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.sesionActiva)
        assertEquals(AppMessage.MSG_01, state.mensajeError)
    }

    @Test
    fun `iniciarSesion no ejecuta caso de uso si los campos estan vacios`() = runTest {
        viewModel.iniciarSesion()
        advanceUntilIdle()

        coVerify(exactly = 0) { useCase(any(), any()) }
    }
}
