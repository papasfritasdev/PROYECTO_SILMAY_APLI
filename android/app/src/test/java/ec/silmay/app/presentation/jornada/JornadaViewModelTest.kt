package ec.silmay.app.presentation.jornada

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Deduccion
import ec.silmay.app.domain.model.EstadoJornada
import ec.silmay.app.domain.model.ItemCatalogo
import ec.silmay.app.domain.model.Jornada
import ec.silmay.app.domain.model.Maquinaria
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.TipoCatalogo
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.CatalogoRepository
import ec.silmay.app.domain.repository.JornadaRepository
import ec.silmay.app.domain.usecase.CerrarJornadaUseCase
import ec.silmay.app.domain.usecase.IniciarJornadaUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JornadaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val authRepository: AuthRepository = mockk()
    private val catalogoRepository: CatalogoRepository = mockk()
    private val jornadaRepository: JornadaRepository = mockk()
    private val iniciarJornadaUseCase: IniciarJornadaUseCase = mockk()
    private val cerrarJornadaUseCase: CerrarJornadaUseCase = mockk()

    private val sesionActiva = UsuarioSesion(
        uid = "usr-1",
        email = "operador@silmay.ec",
        nombres = "Juan Perez",
        cedula = "0922879580",
        rol = Rol.OPERADOR,
        zonaAsignada = "cantera-banos",
        estado = true
    )

    private val maquinaPrueba = Maquinaria(
        codigoInterno = "EX-01",
        marca = "CAT",
        modelo = "320D",
        tipoEquipo = "excavadora",
        horometroActual = 1500.0
    )

    private val lugarPrueba = ItemCatalogo(id = "frente-1", nombre = "Frente 1")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.obtenerSesionActual() } returns sesionActiva
        coEvery { catalogoRepository.obtenerMaquinaria(soloOperativas = true) } returns AppResult.Success(listOf(maquinaPrueba))
        coEvery { catalogoRepository.obtenerItems(TipoCatalogo.LUGARES, soloActivos = true) } returns AppResult.Success(listOf(lugarPrueba))
        coEvery { catalogoRepository.obtenerItems(TipoCatalogo.MOTIVOS, soloActivos = true) } returns AppResult.Success(emptyList())
        coEvery { jornadaRepository.obtenerUltimoHorometroMaquina("EX-01") } returns AppResult.Success(1500.0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cuando no tiene jornada activa carga maquinas y lugares con sugerencia de horometro`() = runTest {
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(null)

        val viewModel = JornadaViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            iniciarJornadaUseCase,
            cerrarJornadaUseCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.jornadaActiva)
        assertEquals(1, state.maquinasDisponibles.size)
        assertEquals(1, state.lugaresDisponibles.size)
        assertEquals("EX-01", state.maquinaSeleccionada?.codigoInterno)
        assertEquals("1500.0", state.horometroInicial)
    }

    @Test
    fun `cuando ya posee jornada activa la expone en el uiState`() = runTest {
        val jornadaAbierta = Jornada(
            uuid = "jornada-abierta-1",
            maquinaId = "EX-01",
            lugarId = "frente-1",
            horaInicioMillis = 1_700_000_000_000L,
            horometroInicial = 1500.0,
            estado = EstadoJornada.ABIERTA,
            userId = "usr-1",
            deviceId = "dev-1",
            zona = "cantera-banos"
        )
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(jornadaAbierta)

        val viewModel = JornadaViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            iniciarJornadaUseCase,
            cerrarJornadaUseCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.jornadaActiva)
        assertEquals("jornada-abierta-1", state.jornadaActiva?.uuid)
    }

    @Test
    fun `iniciarJornada invoca caso de uso y actualiza jornada activa`() = runTest {
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(null)
        val nuevaJornada = Jornada(
            uuid = "nueva-uuid",
            maquinaId = "EX-01",
            lugarId = "frente-1",
            horaInicioMillis = 1_700_000_000_000L,
            horometroInicial = 1500.0,
            estado = EstadoJornada.ABIERTA,
            userId = "usr-1",
            deviceId = "dev-1",
            zona = "cantera-banos"
        )
        coEvery { iniciarJornadaUseCase(any()) } returns AppResult.Success(nuevaJornada)

        val viewModel = JornadaViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            iniciarJornadaUseCase,
            cerrarJornadaUseCase
        )
        advanceUntilIdle()

        viewModel.iniciarJornada()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("nueva-uuid", state.jornadaActiva?.uuid)
        assertTrue(state.operacionExitosa)
        coVerify(exactly = 1) { iniciarJornadaUseCase(any()) }
    }

    @Test
    fun `cerrarJornada exitoso limpia jornada activa y emite operacionExitosa`() = runTest {
        val jornadaAbierta = Jornada(
            uuid = "jornada-abierta-1",
            maquinaId = "EX-01",
            lugarId = "frente-1",
            horaInicioMillis = 1_700_000_000_000L,
            horometroInicial = 1500.0,
            estado = EstadoJornada.ABIERTA,
            userId = "usr-1",
            deviceId = "dev-1",
            zona = "cantera-banos"
        )
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(jornadaAbierta)
        val jornadaCerrada = jornadaAbierta.copy(estado = EstadoJornada.CERRADA)
        coEvery { cerrarJornadaUseCase(any()) } returns AppResult.Success(jornadaCerrada)

        val viewModel = JornadaViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            iniciarJornadaUseCase,
            cerrarJornadaUseCase
        )
        advanceUntilIdle()

        viewModel.alCambiarHorometroFinal("1508.5")
        viewModel.cerrarJornada()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.jornadaActiva)
        assertTrue(state.operacionExitosa)
        coVerify(exactly = 1) { cerrarJornadaUseCase(any()) }
    }

    @Test
    fun `agregar y quitar deducciones modifica la lista del state`() = runTest {
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(null)
        val viewModel = JornadaViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            iniciarJornadaUseCase,
            cerrarJornadaUseCase
        )
        advanceUntilIdle()

        val ded = Deduccion(id = "d1", motivo = "almuerzo", duracionHoras = 1.0)
        viewModel.agregarDeduccion(ded)
        assertEquals(1, viewModel.uiState.value.deducciones.size)

        viewModel.quitarDeduccion("d1")
        assertEquals(0, viewModel.uiState.value.deducciones.size)
    }

    @Test
    fun `error en cierre propaga errorMensaje en el uiState`() = runTest {
        val jornadaAbierta = Jornada(
            uuid = "jornada-abierta-1",
            maquinaId = "EX-01",
            lugarId = "frente-1",
            horaInicioMillis = 1_700_000_000_000L,
            horometroInicial = 1500.0,
            estado = EstadoJornada.ABIERTA,
            userId = "usr-1",
            deviceId = "dev-1",
            zona = "cantera-banos"
        )
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(jornadaAbierta)
        coEvery { cerrarJornadaUseCase(any()) } returns AppResult.Error(AppMessage.MSG_05)

        val viewModel = JornadaViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            iniciarJornadaUseCase,
            cerrarJornadaUseCase
        )
        advanceUntilIdle()

        viewModel.alCambiarHorometroFinal("1400.0") // Inconsistente
        viewModel.cerrarJornada()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AppMessage.MSG_05, state.errorMensaje)
    }
}
