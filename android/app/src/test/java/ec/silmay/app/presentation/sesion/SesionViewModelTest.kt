package ec.silmay.app.presentation.sesion

import ec.silmay.app.data.sync.ConciliadorSincronizacion
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.NetworkMonitor
import ec.silmay.app.domain.repository.SincronizacionRepository
import ec.silmay.app.domain.usecase.CerrarSesionResultado
import ec.silmay.app.domain.usecase.CerrarSesionUseCase
import ec.silmay.app.domain.usecase.ObtenerSesionActualUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val obtenerSesion: ObtenerSesionActualUseCase = mockk()
    private val cerrarSesion: CerrarSesionUseCase = mockk()
    private val networkMonitor: NetworkMonitor = mockk()
    private val sincronizacionRepository: SincronizacionRepository = mockk()
    private val conciliador: ConciliadorSincronizacion = mockk()

    private val pendientesFlow = MutableStateFlow(0)
    private val redFlow = MutableStateFlow(true)

    private val sesion = UsuarioSesion(
        uid = "op-1",
        email = "operador@silmay.ec",
        nombres = "Carlos Operador",
        cedula = "0922879580",
        rol = Rol.OPERADOR,
        zonaAsignada = "cantera-banos",
        estado = true
    )

    private fun crearViewModel(): SesionViewModel = SesionViewModel(
        obtenerSesion,
        cerrarSesion,
        networkMonitor,
        sincronizacionRepository,
        conciliador
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { cerrarSesion() } returns CerrarSesionResultado.Exito
        every { networkMonitor.esOnline } returns redFlow
        every { networkMonitor.estaConectado() } returns true
        every { sincronizacionRepository.observarPendientes(any()) } returns pendientesFlow
        every { conciliador.solicitar() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `arranca en Verificando y pasa a Autenticada si hay sesion persistida`() = runTest {
        coEvery { obtenerSesion() } returns sesion
        val viewModel = crearViewModel()

        assertEquals(SesionUiState.Verificando, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(SesionUiState.Autenticada(sesion), viewModel.uiState.value)
        assertTrue(viewModel.esOnline.value)
    }

    @Test
    fun `sin sesion persistida pasa a SinSesion`() = runTest {
        coEvery { obtenerSesion() } returns null
        val viewModel = crearViewModel()

        advanceUntilIdle()
        assertEquals(SesionUiState.SinSesion, viewModel.uiState.value)
    }

    @Test
    fun `CA1 - login exitoso navega al menu con la sesion del usuario`() = runTest {
        coEvery { obtenerSesion() } returns null
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.onLoginExitoso(sesion)
        advanceUntilIdle()

        assertEquals(SesionUiState.Autenticada(sesion), viewModel.uiState.value)
    }

    @Test
    fun `cerrar sesion exitoso invoca el caso de uso y vuelve a SinSesion`() = runTest {
        coEvery { obtenerSesion() } returns sesion
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.cerrarSesion()
        advanceUntilIdle()

        assertEquals(SesionUiState.SinSesion, viewModel.uiState.value)
        coVerify(exactly = 1) { cerrarSesion() }
    }

    @Test
    fun `HU-07 CA3 - cuando hay registros pendientes bloquea el cierre y emite alerta`() = runTest {
        coEvery { obtenerSesion() } returns sesion
        coEvery { cerrarSesion() } returns CerrarSesionResultado.BloqueadoPorPendientes(2)
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.cerrarSesion()
        advanceUntilIdle()

        assertEquals(SesionUiState.Autenticada(sesion), viewModel.uiState.value)
        assertEquals(2, viewModel.alertaPendientes.value)

        viewModel.limpiarAlertaPendientes()
        assertEquals(null, viewModel.alertaPendientes.value)
    }

    @Test
    fun `HU-08 CA1 - forzarSincronizacion solicita conciliacion al conciliador`() = runTest {
        coEvery { obtenerSesion() } returns sesion
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.forzarSincronizacion()
        verify(exactly = 1) { conciliador.solicitar() }
    }

    @Test
    fun `observarPendientes actualiza reactivamente el flujo de pendientes`() = runTest {
        coEvery { obtenerSesion() } returns sesion
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertEquals(0, viewModel.pendientesSync.value)

        pendientesFlow.value = 3
        advanceUntilIdle()

        assertEquals(3, viewModel.pendientesSync.value)
    }
}
