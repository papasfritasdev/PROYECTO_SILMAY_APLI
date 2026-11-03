package ec.silmay.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RolTest {

    @Test
    fun `solo operador y supervisor estan habilitados en movil`() {
        assertTrue(Rol.OPERADOR.habilitadoEnMovil)
        assertTrue(Rol.SUPERVISOR.habilitadoEnMovil)
        assertFalse(Rol.CONTADOR.habilitadoEnMovil)
        assertFalse(Rol.ADMIN.habilitadoEnMovil)
    }

    @Test
    fun `desdeValor normaliza espacios y mayusculas`() {
        assertEquals(Rol.OPERADOR, Rol.desdeValor(" Operador "))
        assertEquals(Rol.ADMIN, Rol.desdeValor("ADMIN"))
    }

    @Test
    fun `desdeValor retorna null ante valores desconocidos o nulos`() {
        assertNull(Rol.desdeValor("gerente"))
        assertNull(Rol.desdeValor(null))
    }
}
