package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Despacho
import ec.silmay.app.domain.model.EstadoJornada
import ec.silmay.app.domain.model.EstadoValidacion
import ec.silmay.app.domain.model.Jornada
import ec.silmay.app.domain.model.MetodoCobro
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.UnidadMedida
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.DespachoRepository
import ec.silmay.app.domain.repository.DispositivoRepository
import ec.silmay.app.domain.repository.JornadaRepository
import ec.silmay.app.domain.sync.GeneradorUuid
import ec.silmay.app.domain.sync.Reloj
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegistrarDespachoUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val dispositivoRepository: DispositivoRepository = mockk()
    private val jornadaRepository: JornadaRepository = mockk()
    private val despachoRepository: DespachoRepository = mockk()
    private val generadorUuid: GeneradorUuid = GeneradorUuid { "uuid-despacho-1234" }
    private val reloj: Reloj = Reloj { 1_700_000_000_000L }

    private lateinit var useCase: RegistrarDespachoUseCase

    private val sesionActiva = UsuarioSesion(
        uid = "op-1",
        email = "operador@silmay.ec",
        nombres = "Juan Operador",
        cedula = "0922879580",
        rol = Rol.OPERADOR,
        zonaAsignada = "cantera-banos",
        estado = true
    )

    @Before
    fun setUp() {
        useCase = RegistrarDespachoUseCase(
            authRepository = authRepository,
            dispositivoRepository = dispositivoRepository,
            jornadaRepository = jornadaRepository,
            despachoRepository = despachoRepository,
            generadorUuid = generadorUuid,
            reloj = reloj
        )
        coEvery { authRepository.obtenerSesionActual() } returns sesionActiva
        coEvery { dispositivoRepository.obtenerDeviceId() } returns "terminal-01"
    }

    @Test
    fun `registro exitoso con lugar explicito guarda despacho en estado pendiente`() = runTest {
        coEvery { despachoRepository.guardarDespacho(any()) } returns AppResult.Success("uuid-despacho-1234")

        val params = RegistrarDespachoParams(
            materialId = "arena-fina",
            unidadMedida = UnidadMedida.M3,
            cantidad = 12.0,
            clienteId = "constructora-demo",
            volquetaId = "VOL-01",
            lugarId = "frente-norte",
            metodoCobro = MetodoCobro.EFECTIVO,
            valorUSD = 180.00
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Success)
        val despacho = (resultado as AppResult.Success).data
        assertEquals("uuid-despacho-1234", despacho.uuid)
        assertEquals("arena-fina", despacho.materialId)
        assertEquals(UnidadMedida.M3, despacho.unidadMedida)
        assertEquals(12.0, despacho.cantidad, 0.01)
        assertEquals("frente-norte", despacho.lugarId)
        assertEquals(180.00, despacho.valorUSD, 0.01)
        assertEquals(EstadoValidacion.PENDIENTE, despacho.estadoValidacion)
        assertEquals("op-1", despacho.userId)
        assertEquals("terminal-01", despacho.deviceId)
        assertEquals("cantera-banos", despacho.zona)

        coVerify(exactly = 1) { despachoRepository.guardarDespacho(any()) }
    }

    @Test
    fun `HU-03 CA1 - hereda lugar de la jornada activa si no se envia lugar explicito`() = runTest {
        val jornadaActiva = Jornada(
            uuid = "j-activa",
            maquinaId = "EX-01",
            lugarId = "frente-rio-pastaza",
            horaInicioMillis = 1_699_999_000_000L,
            horometroInicial = 1000.0,
            estado = EstadoJornada.ABIERTA,
            userId = "op-1",
            deviceId = "terminal-01",
            zona = "cantera-banos"
        )
        coEvery { jornadaRepository.obtenerJornadaAbierta("op-1") } returns AppResult.Success(jornadaActiva)
        coEvery { despachoRepository.guardarDespacho(any()) } returns AppResult.Success("uuid-despacho-1234")

        val params = RegistrarDespachoParams(
            materialId = "ripio",
            unidadMedida = UnidadMedida.VIAJES,
            cantidad = 2.0,
            clienteId = "cliente-contado",
            volquetaId = "VOL-02",
            lugarId = null, // Hereda de jornada
            metodoCobro = MetodoCobro.TRANSFERENCIA,
            valorUSD = 120.00
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Success)
        val despacho = (resultado as AppResult.Success).data
        assertEquals("frente-rio-pastaza", despacho.lugarId)
    }

    @Test
    fun `falla con MSG_07 cuando valorUSD es menor o igual a cero`() = runTest {
        val params = RegistrarDespachoParams(
            materialId = "arena-fina",
            unidadMedida = UnidadMedida.M3,
            cantidad = 10.0,
            clienteId = "cliente-contado",
            volquetaId = "VOL-01",
            lugarId = "frente-1",
            metodoCobro = MetodoCobro.EFECTIVO,
            valorUSD = 0.0 // Inválido
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_07, (resultado as AppResult.Error).message)
        coVerify(exactly = 0) { despachoRepository.guardarDespacho(any()) }
    }

    @Test
    fun `falla si cantidad es menor o igual a cero`() = runTest {
        val params = RegistrarDespachoParams(
            materialId = "arena-fina",
            unidadMedida = UnidadMedida.M3,
            cantidad = 0.0, // Inválido
            clienteId = "cliente-contado",
            volquetaId = "VOL-01",
            lugarId = "frente-1",
            metodoCobro = MetodoCobro.EFECTIVO,
            valorUSD = 100.0
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_18, (resultado as AppResult.Error).message)
    }

    @Test
    fun `HU-03 CA3 - cuando memoria esta llena propaga MSG_04 sin descartar datos`() = runTest {
        coEvery { despachoRepository.guardarDespacho(any()) } returns AppResult.Error(AppMessage.MSG_04)

        val params = RegistrarDespachoParams(
            materialId = "piedra",
            unidadMedida = UnidadMedida.M3,
            cantidad = 5.0,
            clienteId = "cliente-contado",
            volquetaId = "VOL-01",
            lugarId = "frente-1",
            metodoCobro = MetodoCobro.EFECTIVO,
            valorUSD = 75.00
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_04, (resultado as AppResult.Error).message)
    }
}
