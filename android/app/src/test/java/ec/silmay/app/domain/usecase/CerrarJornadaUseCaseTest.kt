package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.Deduccion
import ec.silmay.app.domain.model.EstadoJornada
import ec.silmay.app.domain.model.Jornada
import ec.silmay.app.domain.model.SeveridadFalla
import ec.silmay.app.domain.repository.JornadaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CerrarJornadaUseCaseTest {

    private val jornadaRepository: JornadaRepository = mockk()
    private lateinit var useCase: CerrarJornadaUseCase

    private val horaInicio = 1_700_000_000_000L
    private val horaSalida = horaInicio + (8 * 3_600_000).toLong() // 8 horas

    private val jornadaAbierta = Jornada(
        uuid = "uuid-jornada-activa",
        maquinaId = "EX-01",
        lugarId = "frente-1",
        horaInicioMillis = horaInicio,
        horometroInicial = 1000.0,
        estado = EstadoJornada.ABIERTA,
        userId = "op-42",
        deviceId = "terminal-01",
        zona = "cantera-banos"
    )

    @Before
    fun setUp() {
        useCase = CerrarJornadaUseCase(jornadaRepository)
        coEvery { jornadaRepository.obtenerJornadaPorUuid("uuid-jornada-activa") } returns AppResult.Success(jornadaAbierta)
        coEvery { jornadaRepository.guardarJornada(any()) } returns AppResult.Success("uuid-jornada-activa")
    }

    @Test
    fun `cierre exitoso calcula metricas cambia estado a CERRADA y persiste`() = runTest {
        val params = CerrarJornadaParams(
            jornadaUuid = "uuid-jornada-activa",
            horaSalidaMillis = horaSalida,
            horometroFinal = 1007.5,
            observaciones = "Operación sin novedades",
            deducciones = listOf(
                Deduccion(id = "d1", motivo = "almuerzo", duracionHoras = 1.0)
            )
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Success)
        val cerrada = (resultado as AppResult.Success).data
        assertEquals(EstadoJornada.CERRADA, cerrada.estado)
        assertEquals(8.0, cerrada.horasBrutas!!, 0.01)
        assertEquals(1.0, cerrada.horasRestadas, 0.01)
        assertEquals(7.0, cerrada.horasEfectivas!!, 0.01)
        assertEquals(7.5, cerrada.deltaHorometro!!, 0.01)
        assertEquals("Operación sin novedades", cerrada.observaciones)

        coVerify(exactly = 1) { jornadaRepository.guardarJornada(any()) }
    }

    @Test
    fun `intento de cerrar jornada inexistente falla con MSG_18`() = runTest {
        coEvery { jornadaRepository.obtenerJornadaPorUuid("inexistente") } returns AppResult.Success(null)

        val params = CerrarJornadaParams(
            jornadaUuid = "inexistente",
            horaSalidaMillis = horaSalida,
            horometroFinal = 1007.5
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_18, (resultado as AppResult.Error).message)
    }

    @Test
    fun `intento de cerrar una jornada ya CERRADA falla por inmutabilidad`() = runTest {
        val yaCerrada = jornadaAbierta.copy(estado = EstadoJornada.CERRADA)
        coEvery { jornadaRepository.obtenerJornadaPorUuid("uuid-jornada-activa") } returns AppResult.Success(yaCerrada)

        val params = CerrarJornadaParams(
            jornadaUuid = "uuid-jornada-activa",
            horaSalidaMillis = horaSalida,
            horometroFinal = 1007.5
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_18, (resultado as AppResult.Error).message)
    }

    @Test
    fun `inconsistencia en horometro final propaga MSG_05`() = runTest {
        val params = CerrarJornadaParams(
            jornadaUuid = "uuid-jornada-activa",
            horaSalidaMillis = horaSalida,
            horometroFinal = 999.0 // Menor al inicial 1000.0
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_05, (resultado as AppResult.Error).message)
    }

    @Test
    fun `inconsistencia en horas propaga MSG_11`() = runTest {
        val params = CerrarJornadaParams(
            jornadaUuid = "uuid-jornada-activa",
            horaSalidaMillis = horaInicio - 1000L, // Salida antes de inicio
            horometroFinal = 1005.0
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_11, (resultado as AppResult.Error).message)
    }

    @Test
    fun `deducciones excesivas propagan MSG_12`() = runTest {
        val params = CerrarJornadaParams(
            jornadaUuid = "uuid-jornada-activa",
            horaSalidaMillis = horaSalida, // 8.0 horas
            horometroFinal = 1005.0,
            deducciones = listOf(
                Deduccion(id = "d1", motivo = "espera", duracionHoras = 8.0) // 8.0 >= 8.0
            )
        )

        val resultado = useCase(params)

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_12, (resultado as AppResult.Error).message)
    }
}
