package ec.silmay.app.presentation.despacho

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Despacho
import ec.silmay.app.domain.model.EstadoJornada
import ec.silmay.app.domain.model.EstadoValidacion
import ec.silmay.app.domain.model.ItemCatalogo
import ec.silmay.app.domain.model.Jornada
import ec.silmay.app.domain.model.Maquinaria
import ec.silmay.app.domain.model.MetodoCobro
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.TipoCatalogo
import ec.silmay.app.domain.model.UnidadMedida
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.CatalogoRepository
import ec.silmay.app.domain.repository.JornadaRepository
import ec.silmay.app.domain.usecase.RegistrarDespachoUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DespachoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val authRepository: AuthRepository = mockk()
    private val catalogoRepository: CatalogoRepository = mockk()
    private val jornadaRepository: JornadaRepository = mockk()
    private val registrarDespachoUseCase: RegistrarDespachoUseCase = mockk()

    private val sesionActiva = UsuarioSesion(
        uid = "usr-1",
        email = "operador@silmay.ec",
        nombres = "Juan Operador",
        cedula = "0922879580",
        rol = Rol.OPERADOR,
        zonaAsignada = "cantera-banos",
        estado = true
    )

    private val material1 = ItemCatalogo(id = "arena-fina", nombre = "Arena Fina")
    private val cliente1 = ItemCatalogo(id = "cliente-1", nombre = "Constructora Demo")
    private val volqueta1 = Maquinaria(
        codigoInterno = "VOL-01",
        marca = "Hino",
        modelo = "500",
        tipoEquipo = "volqueta",
        placa = "ABC-1234"
    )
    private val excavadora1 = Maquinaria(
        codigoInterno = "EX-01",
        marca = "CAT",
        modelo = "320D",
        tipoEquipo = "excavadora"
    )
    private val lugar1 = ItemCatalogo(id = "frente-norte", nombre = "Frente Norte")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.obtenerSesionActual() } returns sesionActiva
        coEvery { catalogoRepository.obtenerItems(TipoCatalogo.MATERIALES, soloActivos = true) } returns AppResult.Success(listOf(material1))
        coEvery { catalogoRepository.obtenerItems(TipoCatalogo.CLIENTES, soloActivos = true) } returns AppResult.Success(listOf(cliente1))
        coEvery { catalogoRepository.obtenerMaquinaria(soloOperativas = true) } returns AppResult.Success(listOf(volqueta1, excavadora1))
        coEvery { catalogoRepository.obtenerItems(TipoCatalogo.LUGARES, soloActivos = true) } returns AppResult.Success(listOf(lugar1))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init carga catalogos y filtra solo volquetas en la lista de volquetas`() = runTest {
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(null)

        val viewModel = DespachoViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            registrarDespachoUseCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.cargando)
        assertEquals(1, state.materiales.size)
        assertEquals(1, state.clientes.size)
        // De 2 maquinas (volqueta y excavadora), solo 1 debe ser volqueta
        assertEquals(1, state.volquetas.size)
        assertEquals("VOL-01", state.volquetas.first().codigoInterno)
        assertEquals("arena-fina", state.materialSeleccionado?.id)
        assertEquals("cliente-1", state.clienteSeleccionado?.id)
        assertEquals("VOL-01", state.volquetaSeleccionada?.codigoInterno)
    }

    @Test
    fun `init con jornada activa detecta y hereda el lugar de la jornada`() = runTest {
        val jornadaActiva = Jornada(
            uuid = "jornada-1",
            maquinaId = "EX-01",
            lugarId = "frente-norte",
            horaInicioMillis = 1_700_000_000_000L,
            horometroInicial = 1000.0,
            estado = EstadoJornada.ABIERTA,
            userId = "usr-1",
            deviceId = "dev-1",
            zona = "cantera-banos"
        )
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(jornadaActiva)

        val viewModel = DespachoViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            registrarDespachoUseCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.jornadaActiva)
        assertEquals("frente-norte", state.lugarSeleccionado?.id)
    }

    @Test
    fun `guardarDespacho con campos vacios dispara alerta haptica y no invoca use case`() = runTest {
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(null)

        val viewModel = DespachoViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            registrarDespachoUseCase
        )
        advanceUntilIdle()

        var hapticaDisparada = false
        viewModel.guardarDespacho(alertaHaptica = { hapticaDisparada = true })
        advanceUntilIdle()

        assertTrue(hapticaDisparada)
        assertTrue(viewModel.uiState.value.errorCamposIncompletos)
        coVerify(exactly = 0) { registrarDespachoUseCase(any()) }
    }

    @Test
    fun `guardarDespacho con valor menor o igual a cero emite MSG_07`() = runTest {
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(null)

        val viewModel = DespachoViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            registrarDespachoUseCase
        )
        advanceUntilIdle()

        viewModel.alCambiarCantidad("10.0")
        viewModel.alCambiarValorUSD("0.0") // Inválido

        var hapticaDisparada = false
        viewModel.guardarDespacho(alertaHaptica = { hapticaDisparada = true })
        advanceUntilIdle()

        assertTrue(hapticaDisparada)
        assertEquals(AppMessage.MSG_07, viewModel.uiState.value.errorMensaje)
        coVerify(exactly = 0) { registrarDespachoUseCase(any()) }
    }

    @Test
    fun `guardarDespacho exitoso invoca use case y limpia formulario`() = runTest {
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(null)
        val despachoCreado = Despacho(
            uuid = "despacho-1",
            materialId = "arena-fina",
            unidadMedida = UnidadMedida.M3,
            cantidad = 5.0,
            clienteId = "cliente-1",
            volquetaId = "VOL-01",
            lugarId = "frente-norte",
            metodoCobro = MetodoCobro.EFECTIVO,
            valorUSD = 80.0,
            fechaHoraMillis = 1_700_000_000_000L,
            estadoValidacion = EstadoValidacion.PENDIENTE,
            userId = "usr-1",
            deviceId = "dev-1",
            zona = "cantera-banos"
        )
        coEvery { registrarDespachoUseCase(any()) } returns AppResult.Success(despachoCreado)

        val viewModel = DespachoViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            registrarDespachoUseCase
        )
        advanceUntilIdle()

        viewModel.alCambiarCantidad("5.0")
        viewModel.alCambiarValorUSD("80.0")

        var exito = false
        viewModel.guardarDespacho(onExito = { exito = true })
        advanceUntilIdle()

        assertTrue(exito)
        assertTrue(viewModel.uiState.value.operacionExitosa)
        assertEquals("", viewModel.uiState.value.cantidad)
        assertEquals("", viewModel.uiState.value.valorUSD)
        coVerify(exactly = 1) { registrarDespachoUseCase(any()) }
    }

    @Test
    fun `guardarDespacho con error de memoria llena MSG_04 propaga error y dispara haptica`() = runTest {
        coEvery { jornadaRepository.obtenerJornadaAbierta("usr-1") } returns AppResult.Success(null)
        coEvery { registrarDespachoUseCase(any()) } returns AppResult.Error(AppMessage.MSG_04)

        val viewModel = DespachoViewModel(
            authRepository,
            catalogoRepository,
            jornadaRepository,
            registrarDespachoUseCase
        )
        advanceUntilIdle()

        viewModel.alCambiarCantidad("5.0")
        viewModel.alCambiarValorUSD("80.0")

        var hapticaDisparada = false
        viewModel.guardarDespacho(alertaHaptica = { hapticaDisparada = true })
        advanceUntilIdle()

        assertTrue(hapticaDisparada)
        assertEquals(AppMessage.MSG_04, viewModel.uiState.value.errorMensaje)
        // HU-03 CA3: no descarta los datos digitados en el formulario
        assertEquals("5.0", viewModel.uiState.value.cantidad)
        assertEquals("80.0", viewModel.uiState.value.valorUSD)
    }
}
