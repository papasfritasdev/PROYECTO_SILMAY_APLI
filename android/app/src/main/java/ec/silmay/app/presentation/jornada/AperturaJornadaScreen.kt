package ec.silmay.app.presentation.jornada

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ec.silmay.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AperturaJornadaScreen(
    viewModel: JornadaViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var expandoMaquinas by remember { mutableStateOf(false) }
    var expandoLugares by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.jornada_titulo_apertura)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.jornada_volver)
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.cargando) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMensaje?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error.texto,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Selector de Maquinaria
            Text(
                text = stringResource(R.string.jornada_maquina),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ExposedDropdownMenuBox(
                expanded = expandoMaquinas,
                onExpandedChange = { expandoMaquinas = !expandoMaquinas },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.maquinaSeleccionada?.let { "${it.codigoInterno} - ${it.marca} ${it.modelo}" } ?: "Seleccionar máquina",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandoMaquinas) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandoMaquinas,
                    onDismissRequest = { expandoMaquinas = false }
                ) {
                    state.maquinasDisponibles.forEach { maq ->
                        DropdownMenuItem(
                            text = { Text("${maq.codigoInterno} - ${maq.marca} ${maq.modelo} (${maq.tipoEquipo})") },
                            onClick = {
                                viewModel.alSeleccionarMaquina(maq)
                                expandoMaquinas = false
                            }
                        )
                    }
                }
            }

            // Selector de Frente / Lugar
            Text(
                text = stringResource(R.string.jornada_lugar),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ExposedDropdownMenuBox(
                expanded = expandoLugares,
                onExpandedChange = { expandoLugares = !expandoLugares },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.lugarSeleccionado?.nombre ?: "Seleccionar frente de trabajo",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandoLugares) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandoLugares,
                    onDismissRequest = { expandoLugares = false }
                ) {
                    state.lugaresDisponibles.forEach { lug ->
                        DropdownMenuItem(
                            text = { Text(lug.nombre) },
                            onClick = {
                                viewModel.alSeleccionarLugar(lug)
                                expandoLugares = false
                            }
                        )
                    }
                }
            }

            // Horómetro Inicial (con sugerencia automática HU-04 CA1)
            Text(
                text = stringResource(R.string.jornada_horometro_inicial),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = state.horometroInicial,
                onValueChange = viewModel::alCambiarHorometroInicial,
                label = { Text("Lectura de horómetro actual") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.iniciarJornada() },
                enabled = !state.cargando && state.maquinaSeleccionada != null && state.lugarSeleccionado != null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = stringResource(R.string.jornada_btn_iniciar),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            OutlinedButton(
                onClick = onVolver,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Cancelar",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
