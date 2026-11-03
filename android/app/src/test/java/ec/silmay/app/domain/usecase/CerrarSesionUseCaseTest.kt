package ec.silmay.app.domain.usecase

import ec.silmay.app.domain.model.AppResult
import ec.silmay.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CerrarSesionUseCaseTest {

    @Test
    fun `delega el cierre de sesion en el repositorio`() = runTest {
        val authRepository: AuthRepository = mockk()
        coEvery { authRepository.cerrarSesion() } returns AppResult.Success(Unit)

        val resultado = CerrarSesionUseCase(authRepository)()

        assertEquals(AppResult.Success(Unit), resultado)
        coVerify(exactly = 1) { authRepository.cerrarSesion() }
    }
}
