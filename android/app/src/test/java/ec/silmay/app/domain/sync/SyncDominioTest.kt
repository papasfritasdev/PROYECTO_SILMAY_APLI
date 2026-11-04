package ec.silmay.app.domain.sync

import ec.silmay.app.domain.model.ColeccionOperativa
import ec.silmay.app.domain.model.MetadatosControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/** Reglas puras de la capa de sincronización (HT-07, ADR-0005). */
class SyncDominioTest {

    @Test
    fun `UUID v4 generado por la JVM es valido`() {
        repeat(100) { assertTrue(UuidV4.esValido(UUID.randomUUID().toString())) }
    }

    @Test
    fun `se rechazan IDs que no son UUID v4 canonico`() {
        listOf(
            "",
            "abc",
            "123",
            "6ba7b810-9dad-11d1-80b4-00c04fd430c8", // v1
            "6BA7B810-9DAD-41D1-80B4-00C04FD430C8", // mayúsculas
            "6ba7b810-9dad-41d1-c0b4-00c04fd430c8", // variante inválida
            "6ba7b8109dad41d180b400c04fd430c8" // sin guiones
        ).forEach { assertFalse("'$it' no debe aceptarse", UuidV4.esValido(it)) }
    }

    @Test
    fun `documento existente en servidor se confirma`() {
        assertEquals(
            ResolucionPendiente.CONFIRMADO,
            ResolucionPendiente.resolver(existeEnServidor = true, lecturaDenegada = false)
        )
    }

    @Test
    fun `documento inexistente o ilegible tras vaciar la cola es un rechazo`() {
        assertEquals(
            ResolucionPendiente.RECHAZADO,
            ResolucionPendiente.resolver(existeEnServidor = false, lecturaDenegada = false)
        )
        assertEquals(
            ResolucionPendiente.RECHAZADO,
            ResolucionPendiente.resolver(existeEnServidor = null, lecturaDenegada = true)
        )
    }

    @Test
    fun `lectura no completada queda indeterminada`() {
        assertEquals(
            ResolucionPendiente.INDETERMINADO,
            ResolucionPendiente.resolver(existeEnServidor = null, lecturaDenegada = false)
        )
    }

    @Test
    fun `los campos de control son exactamente los 6 del esquema`() {
        assertEquals(
            setOf("uuid", "createdAtClient", "syncedAtServer", "userId", "deviceId", "zona"),
            MetadatosControl.CAMPOS_CONTROL
        )
    }

    @Test
    fun `colecciones operativas se resuelven por su nombre canonico`() {
        assertEquals(ColeccionOperativa.JORNADAS, ColeccionOperativa.desdeValor("jornadas"))
        assertEquals(ColeccionOperativa.DESPACHOS, ColeccionOperativa.desdeValor("despachos"))
        assertEquals(ColeccionOperativa.GASTOS, ColeccionOperativa.desdeValor("gastos"))
        assertNull(ColeccionOperativa.desdeValor("maquinaria"))
        assertNull(ColeccionOperativa.desdeValor(null))
    }
}
