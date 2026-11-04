package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppMessage
import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.model.ColeccionOperativa
import ec.silmay.app.domain.model.MetadatosControl
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.UsuarioSesion
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.DispositivoRepository
import ec.silmay.app.domain.repository.RegistroOperativoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/** HT-07 §1–§2: inyección de los metadatos de control en cada registro operativo. */
class GuardarRegistroOperativoUseCaseTest {

    private val uuidFijo = "3f2b8c1e-5d4a-4b6c-9e7f-1a2b3c4d5e6f"
    private val ahora = 1_762_000_000_000L

    private lateinit var auth: AuthRepository
    private lateinit var dispositivo: DispositivoRepository
    private lateinit var registros: RegistroOperativoRepository
    private var uuidGenerado = uuidFijo
    private lateinit var useCase: GuardarRegistroOperativoUseCase

    private fun sesion(
        rol: Rol = Rol.OPERADOR,
        estado: Boolean = true,
        zona: String = "cantera-banos"
    ) = UsuarioSesion("uid-op", "op@silmay.ec", "Operador", "0102030405", rol, zona, estado)

    @Before
    fun setUp() {
        auth = mockk()
        dispositivo = mockk()
        registros = mockk()
        every { dispositivo.obtenerDeviceId() } returns "device-1"
        coEvery { auth.obtenerSesionActual() } returns sesion()
        coEvery { registros.guardar(any(), any(), any()) } answers {
            AppResult.Success(secondArg<MetadatosControl>().uuid)
        }
        useCase = GuardarRegistroOperativoUseCase(
            auth, dispositivo, registros, { uuidGenerado }, { ahora }
        )
    }

    @Test
    fun `inyecta uuid, createdAtClient, userId, deviceId y zona desde la sesion`() = runTest {
        val metadatos = slot<MetadatosControl>()
        coEvery { registros.guardar(any(), capture(metadatos), any()) } returns AppResult.Success(uuidFijo)

        val resultado = useCase(ColeccionOperativa.GASTOS, mapOf("rubro" to "combustible"))

        assertEquals(AppResult.Success(uuidFijo), resultado)
        assertEquals(
            MetadatosControl(uuidFijo, ahora, "uid-op", "device-1", "cantera-banos"),
            metadatos.captured
        )
    }

    @Test
    fun `descarta claves de control enviadas por el llamador`() = runTest {
        val datos = slot<Map<String, Any?>>()
        coEvery { registros.guardar(any(), any(), capture(datos)) } returns AppResult.Success(uuidFijo)

        useCase(
            ColeccionOperativa.GASTOS,
            mapOf(
                "rubro" to "combustible",
                "userId" to "otro-usuario",
                "zona" to "otra-zona",
                "uuid" to "id-elegido",
                "deviceId" to "x",
                "createdAtClient" to 0,
                "syncedAtServer" to 0
            )
        )

        assertEquals(mapOf("rubro" to "combustible"), datos.captured)
    }

    @Test
    fun `cada guardado genera un UUID nuevo (nunca add)`() = runTest {
        val ids = mutableListOf<String>()
        coEvery { registros.guardar(any(), any(), any()) } answers {
            ids += secondArg<MetadatosControl>().uuid
            AppResult.Success(ids.last())
        }

        uuidGenerado = "11111111-1111-4111-8111-111111111111"
        useCase(ColeccionOperativa.DESPACHOS, emptyMap())
        uuidGenerado = "22222222-2222-4222-a222-222222222222"
        useCase(ColeccionOperativa.DESPACHOS, emptyMap())

        assertEquals(2, ids.distinct().size)
    }

    @Test
    fun `sin sesion persistida no guarda`() = runTest {
        coEvery { auth.obtenerSesionActual() } returns null

        val resultado = useCase(ColeccionOperativa.GASTOS, emptyMap())

        assertEquals(AppMessage.MSG_18, (resultado as AppResult.Error).message)
        coVerify(exactly = 0) { registros.guardar(any(), any(), any()) }
    }

    @Test
    fun `roles de oficina o cuentas inactivas no capturan en movil (MSG-13)`() = runTest {
        listOf(sesion(rol = Rol.CONTADOR), sesion(rol = Rol.ADMIN), sesion(estado = false))
            .forEach { s ->
                coEvery { auth.obtenerSesionActual() } returns s
                val resultado = useCase(ColeccionOperativa.GASTOS, emptyMap())
                assertEquals(AppMessage.MSG_13, (resultado as AppResult.Error).message)
            }
        coVerify(exactly = 0) { registros.guardar(any(), any(), any()) }
    }

    @Test
    fun `el supervisor tambien puede capturar`() = runTest {
        coEvery { auth.obtenerSesionActual() } returns sesion(rol = Rol.SUPERVISOR)

        assertEquals(AppResult.Success(uuidFijo), useCase(ColeccionOperativa.JORNADAS, emptyMap()))
    }

    @Test
    fun `zona vacia, deviceId vacio o UUID invalido bloquean el guardado`() = runTest {
        coEvery { auth.obtenerSesionActual() } returns sesion(zona = " ")
        assertFalse(useCase(ColeccionOperativa.GASTOS, emptyMap()) is AppResult.Success)

        coEvery { auth.obtenerSesionActual() } returns sesion()
        every { dispositivo.obtenerDeviceId() } returns ""
        assertFalse(useCase(ColeccionOperativa.GASTOS, emptyMap()) is AppResult.Success)

        every { dispositivo.obtenerDeviceId() } returns "device-1"
        uuidGenerado = "no-es-uuid"
        assertFalse(useCase(ColeccionOperativa.GASTOS, emptyMap()) is AppResult.Success)

        coVerify(exactly = 0) { registros.guardar(any(), any(), any()) }
    }

    @Test
    fun `propaga el error del repositorio (MSG-04 memoria insuficiente)`() = runTest {
        coEvery { registros.guardar(any(), any(), any()) } returns AppResult.Error(AppMessage.MSG_04)

        val resultado = useCase(ColeccionOperativa.GASTOS, emptyMap())

        assertEquals(AppMessage.MSG_04, (resultado as AppResult.Error).message)
    }
}
