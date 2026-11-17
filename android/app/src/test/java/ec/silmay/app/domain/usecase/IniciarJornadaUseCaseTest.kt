package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.EstadoJornada
import ec.silmay.app.domain.model.Jornada
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.DispositivoRepository
import ec.silmay.app.domain.repository.JornadaRepository
import ec.silmay.app.domain.sync.GeneradorUuid
import ec.silmay.app.domain.sync.Reloj
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IniciarJornadaUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val dispositivoRepository: DispositivoRepository = mockk()
    private val jornadaRepository: JornadaRepository = mockk()
    private val generadorUuid: GeneradorUuid = GeneradorUuid { "uuid-jornada-1234" }
    private val reloj: Reloj = Reloj { 1_700_000_000_000L }

    private lateinit var useCase: IniciarJornadaUseCase

    private val sesionActiva = UsuarioSesion(
        uid = "op-42",
        email = "operador@silmay.ec",
        nombres = "Manuel Operador",
        cedula = "0922879580",
        rol = Rol.OPERADOR,
        zonaAsignada = "cantera-banos",
        estado = true
    )

    @Before
    fun setUp() {
        useCase = IniciarJornadaUseCase(
            authRepository = authRepository,
            dispositivoRepository = dispositivoRepository,
            jornadaRepository = jornadaRepository,
            generadorUuid = generadorUuid,
            reloj = reloj
        )
        coEvery { authRepository.obtenerSesionActual() } returns sesionActiva
        coEvery { dispositivoRepository.obtenerDeviceId() } returns "terminal-01"
    }

    @Test
    fun `cuando no tiene jornada abierta crea e inicia una nueva jornada con exito`() = runTest {
        coEvery { jornadaRepository.obtenerJornadaAbierta(sesionActiva.uid) } returns AppResult.Success(null)
        coEvery { jornadaRepository.guardarJornada(any()) } returns AppResult.Success("uuid-jornada-1234")

        val params = IniciarJornadaParams(
            maquinaId = "EX-01",
            lugarId = "frente-1",
            horometroInicial = 1250.5
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Success)
        val jornada = (resultado as AppResult.Success).data
        assertEquals("uuid-jornada-1234", jornada.uuid)
        assertEquals("EX-01", jornada.maquinaId)
        assertEquals("frente-1", jornada.lugarId)
        assertEquals(1250.5, jornada.horometroInicial, 0.01)
        assertEquals(EstadoJornada.ABIERTA, jornada.estado)
        assertEquals("op-42", jornada.userId)
        assertEquals("terminal-01", jornada.deviceId)
        assertEquals("cantera-banos", jornada.zona)
        assertFalse(jornada.tieneFallaCritica)
        assertFalse(jornada.procesada)

        coVerify(exactly = 1) { jornadaRepository.guardarJornada(any()) }
    }

    @Test
    fun `HU-04 CA4 - cuando ya posee una jornada abierta bloquea con MSG_14`() = runTest {
        val jornadaExistente = Jornada(
            uuid = "jornada-previa",
            maquinaId = "EX-02",
            lugarId = "frente-2",
            horaInicioMillis = 1_699_999_000_000L,
            horometroInicial = 800.0,
            estado = EstadoJornada.ABIERTA,
            userId = sesionActiva.uid,
            deviceId = "terminal-01",
            zona = "cantera-banos"
        )
        coEvery { jornadaRepository.obtenerJornadaAbierta(sesionActiva.uid) } returns AppResult.Success(jornadaExistente)

        val params = IniciarJornadaParams(
            maquinaId = "EX-01",
            lugarId = "frente-1",
            horometroInicial = 1250.5
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_14, (resultado as AppResult.Error).message)
        coVerify(exactly = 0) { jornadaRepository.guardarJornada(any()) }
    }

    @Test
    fun `sin sesion activa falla con error generico MSG_18`() = runTest {
        coEvery { authRepository.obtenerSesionActual() } returns null

        val params = IniciarJornadaParams(
            maquinaId = "EX-01",
            lugarId = "frente-1",
            horometroInicial = 1250.5
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_18, (resultado as AppResult.Error).message)
    }
}
