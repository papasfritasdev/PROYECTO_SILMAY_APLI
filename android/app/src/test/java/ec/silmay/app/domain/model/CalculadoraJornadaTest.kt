package ec.silmay.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculadoraJornadaTest {

    private val horaInicio = 1_700_000_000_000L // T0
    private val horaSalida8HorasMedia = horaInicio + (8.5 * 3_600_000).toLong() // T0 + 8.5 h

    @Test
    fun `calculo normal sin deducciones computa horas brutas efectivas y delta horometro`() {
        val resultado = CalculadoraJornada.calcularCierre(
            horaInicioMillis = horaInicio,
            horaSalidaMillis = horaSalida8HorasMedia,
            horometroInicial = 1000.0,
            horometroFinal = 1007.8,
            deducciones = emptyList()
        )

        assertTrue(resultado is AppResult.Success)
        val data = (resultado as AppResult.Success).data
        assertEquals(8.5, data.horasBrutas, 0.01)
        assertEquals(0.0, data.horasRestadas, 0.01)
        assertEquals(8.5, data.horasEfectivas, 0.01)
        assertEquals(7.8, data.deltaHorometro, 0.01)
        assertFalse(data.tieneFallaCritica)
    }

    @Test
    fun `calculo con deducciones normales resta correctamente el tiempo improductivo`() {
        val deducciones = listOf(
            Deduccion(id = "d1", motivo = "almuerzo", duracionHoras = 1.0),
            Deduccion(id = "d2", motivo = "clima", duracionHoras = 0.5)
        )

        val resultado = CalculadoraJornada.calcularCierre(
            horaInicioMillis = horaInicio,
            horaSalidaMillis = horaSalida8HorasMedia,
            horometroInicial = 1000.0,
            horometroFinal = 1006.5,
            deducciones = deducciones
        )

        assertTrue(resultado is AppResult.Success)
        val data = (resultado as AppResult.Success).data
        assertEquals(8.5, data.horasBrutas, 0.01)
        assertEquals(1.5, data.horasRestadas, 0.01)
        assertEquals(7.0, data.horasEfectivas, 0.01)
        assertEquals(6.5, data.deltaHorometro, 0.01)
    }

    @Test
    fun `bloquea cierre con MSG_11 cuando horaSalida es menor o igual a horaInicio`() {
        val resultado = CalculadoraJornada.calcularCierre(
            horaInicioMillis = horaInicio,
            horaSalidaMillis = horaInicio, // Igual
            horometroInicial = 1000.0,
            horometroFinal = 1005.0,
            deducciones = emptyList()
        )

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_11, (resultado as AppResult.Error).message)
    }

    @Test
    fun `bloquea cierre con MSG_05 cuando horometroFinal es menor o igual a horometroInicial`() {
        val resultado = CalculadoraJornada.calcularCierre(
            horaInicioMillis = horaInicio,
            horaSalidaMillis = horaSalida8HorasMedia,
            horometroInicial = 1000.0,
            horometroFinal = 999.5, // Menor
            deducciones = emptyList()
        )

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_05, (resultado as AppResult.Error).message)
    }

    @Test
    fun `bloquea cierre con MSG_06 cuando una falla tecnica tiene descripcion menor a 10 caracteres`() {
        val deducciones = listOf(
            Deduccion(
                id = "d1",
                motivo = "falla-mecanica",
                duracionHoras = 1.0,
                esFalla = true,
                severidad = SeveridadFalla.LEVE,
                descripcion = "Corta" // Menor a 10 caracteres
            )
        )

        val resultado = CalculadoraJornada.calcularCierre(
            horaInicioMillis = horaInicio,
            horaSalidaMillis = horaSalida8HorasMedia,
            horometroInicial = 1000.0,
            horometroFinal = 1006.0,
            deducciones = deducciones
        )

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_06, (resultado as AppResult.Error).message)
    }

    @Test
    fun `bloquea cierre con MSG_12 cuando las horas restadas igualan o superan las horas brutas`() {
        val deducciones = listOf(
            Deduccion(id = "d1", motivo = "traslado", duracionHoras = 8.5)
        )

        val resultado = CalculadoraJornada.calcularCierre(
            horaInicioMillis = horaInicio,
            horaSalidaMillis = horaSalida8HorasMedia, // 8.5 horas
            horometroInicial = 1000.0,
            horometroFinal = 1006.0,
            deducciones = deducciones
        )

        assertTrue(resultado is AppResult.Error)
        assertEquals(AppMessage.MSG_12, (resultado as AppResult.Error).message)
    }

    @Test
    fun `detecta tieneFallaCritica true cuando existe al menos una falla con severidad critica`() {
        val deducciones = listOf(
            Deduccion(
                id = "d1",
                motivo = "falla-hidraulica",
                duracionHoras = 2.0,
                esFalla = true,
                severidad = SeveridadFalla.CRITICA,
                descripcion = "Fuga masiva de aceite hidráulico en brazo principal"
            )
        )

        val resultado = CalculadoraJornada.calcularCierre(
            horaInicioMillis = horaInicio,
            horaSalidaMillis = horaSalida8HorasMedia,
            horometroInicial = 1000.0,
            horometroFinal = 1006.0,
            deducciones = deducciones
        )

        assertTrue(resultado is AppResult.Success)
        assertTrue((resultado as AppResult.Success).data.tieneFallaCritica)
    }
}
