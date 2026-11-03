package ec.silmay.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ec.silmay.app.presentation.login.LoginScreen
import ec.silmay.app.presentation.login.LoginViewModel
import ec.silmay.app.presentation.menu.MenuPrincipalScreen
import ec.silmay.app.presentation.sesion.SesionUiState
import ec.silmay.app.presentation.sesion.SesionViewModel

object Rutas {
    const val LOGIN = "login"
    const val MENU = "menu"
}

/**
 * Grafo de navegación raíz. La sesión (SesionViewModel) es la única fuente de verdad:
 * cualquier cambio de estado redirige a la ruta correspondiente limpiando el back stack.
 */
@Composable
fun SilmayNavHost(
    sesionViewModel: SesionViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val estado by sesionViewModel.uiState.collectAsState()

    if (estado is SesionUiState.Verificando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val rutaInicial = if (estado is SesionUiState.Autenticada) Rutas.MENU else Rutas.LOGIN

    LaunchedEffect(estado) {
        val destino = if (estado is SesionUiState.Autenticada) Rutas.MENU else Rutas.LOGIN
        if (navController.currentDestination?.route != destino) {
            navController.navigate(destino) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = rutaInicial) {
        composable(Rutas.LOGIN) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = sesionViewModel::onLoginExitoso
            )
        }
        composable(Rutas.MENU) {
            val sesion = (estado as? SesionUiState.Autenticada)?.sesion
            if (sesion != null) {
                MenuPrincipalScreen(
                    sesion = sesion,
                    onCerrarSesion = sesionViewModel::cerrarSesion
                )
            }
        }
    }
}
