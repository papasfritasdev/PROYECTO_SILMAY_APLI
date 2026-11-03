package ec.silmay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import ec.silmay.app.presentation.login.LoginScreen
import ec.silmay.app.presentation.login.LoginViewModel
import ec.silmay.app.ui.theme.SilmayTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SilmayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val loginViewModel: LoginViewModel = hiltViewModel()
                    LoginScreen(
                        viewModel = loginViewModel,
                        onLoginSuccess = { sesion ->
                            // Navegación al dashboard según rol (operador / supervisor)
                        }
                    )
                }
            }
        }
    }
}
