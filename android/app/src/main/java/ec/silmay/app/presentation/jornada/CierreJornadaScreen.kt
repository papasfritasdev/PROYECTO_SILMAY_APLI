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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TextButton
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
import ec.silmay.app.domain.model.Deduccion
import ec.silmay.app.domain.model.SeveridadFalla
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreJornadaScreen(
    viewModel: JornadaViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val jornada = state.jornadaActiva

    var mostrarDialogoDeduccion by remember { mutableStateOf(false) }

    // Campos temporales para nueva deducción
    var motivoSeleccionado by remember { mutableStateOf(state.motivosDeduccion.firstOrNull()?.nombre ?: "Almuerzo") }
    var duracionTexto by remember { mutableStateOf("0.5") }
    var esFalla by remember { mutableStateOf(false) }
    var severidadSeleccionada by remember { mutableStateOf(SeveridadFalla.LEVE) }
    var descripcionFalla by remember { mutableStateOf("") }
    var expandoMotivos by remember { mutableStateOf(false) }
    var expandoSeveridad by remember { mutableStateOf(false) }

    val formatoHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.jornada_titulo_cierre)) },
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

            // Tarjeta de resumen de Jornada Abierta
            if (jornada != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Jornada en Curso: ${jornada.maquinaId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Frente: ${jornada.lugarId} | Inicio: ${formatoHora.format(Date(jornada.horaInicioMillis))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Horómetro Inicial: ${jornada.horometroInicial}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Campo de Horómetro Final
            Text(
                text = stringResource(R.string.jornada_horometro_final),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = state.horometroFinal,
                onValueChange = viewModel::alCambiarHorometroFinal,
                label = { Text("Lectura al terminar el turno") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Observaciones
            Text(
                text = stringResource(R.string.jornada_observaciones),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = state.observaciones,
                onValueChange = viewModel::alCambiarObservaciones,
                label = { Text("Novedades generales del turno") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Sección Deducciones / Novedades
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.jornada_deducciones_titulo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    onClick = { mostrarDialogoDeduccion = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Deducción")
                }
            }

            state.deducciones.forEach { ded ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (ded.esFalla) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${ded.motivo} - ${ded.duracionHoras} h",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (ded.esFalla) {
                                Text(
                                    text = "Falla [${ded.severidad?.valor}]: ${ded.descripcion ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.quitarDeduccion(ded.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Cierre
            Button(
                onClick = { viewModel.cerrarJornada(onExito = onVolver) },
                enabled = !state.cargando && state.horometroFinal.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(R.string.jornada_btn_cerrar),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    // Diálogo para Añadir Deducción
    if (mostrarDialogoDeduccion) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoDeduccion = false },
            title = { Text(text = stringResource(R.string.jornada_btn_agregar_deduccion)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Selector de Motivo
                    ExposedDropdownMenuBox(
                        expanded = expandoMotivos,
                        onExpandedChange = { expandoMotivos = !expandoMotivos }
                    ) {
                        OutlinedTextField(
                            value = motivoSeleccionado,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Motivo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandoMotivos) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandoMotivos,
                            onDismissRequest = { expandoMotivos = false }
                        ) {
                            state.motivosDeduccion.forEach { mot ->
                                DropdownMenuItem(
                                    text = { Text(mot.nombre) },
                                    onClick = {
                                        motivoSeleccionado = mot.nombre
                                        expandoMotivos = false
                                    }
                                )
                            }
                        }
                    }

                    // Duración en horas
                    OutlinedTextField(
                        value = duracionTexto,
                        onValueChange = { duracionTexto = it },
                        label = { Text(stringResource(R.string.jornada_duracion_horas)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Es falla técnica
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = esFalla, onCheckedChange = { esFalla = it })
                        Text(text = stringResource(R.string.jornada_es_falla))
                    }

                    if (esFalla) {
                        // Selector de Severidad
                        ExposedDropdownMenuBox(
                            expanded = expandoSeveridad,
                            onExpandedChange = { expandoSeveridad = !expandoSeveridad }
                        ) {
                            OutlinedTextField(
                                value = severidadSeleccionada.valor.uppercase(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.jornada_severidad)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandoSeveridad) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandoSeveridad,
                                onDismissRequest = { expandoSeveridad = false }
                            ) {
                                SeveridadFalla.entries.forEach { sev ->
                                    DropdownMenuItem(
                                        text = { Text(sev.valor.uppercase()) },
                                        onClick = {
                                            severidadSeleccionada = sev
                                            expandoSeveridad = false
                                        }
                                    )
                                }
                            }
                        }

                        // Descripción de falla con contador (mínimo 10 caracteres)
                        OutlinedTextField(
                            value = descripcionFalla,
                            onValueChange = { descripcionFalla = it },
                            label = { Text(stringResource(R.string.jornada_descripcion_falla)) },
                            supportingText = { Text("${descripcionFalla.length}/10 caracteres mínimos") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val duracion = duracionTexto.toDoubleOrNull() ?: 0.0
                        if (duracion > 0.0) {
                            viewModel.agregarDeduccion(
                                Deduccion(
                                    id = UUID.randomUUID().toString(),
                                    motivo = motivoSeleccionado,
                                    duracionHoras = duracion,
                                    esFalla = esFalla,
                                    severidad = if (esFalla) severidadSeleccionada else null,
                                    descripcion = if (esFalla) descripcionFalla else null
                                )
                            )
                            mostrarDialogoDeduccion = false
                            // Reset
                            duracionTexto = "0.5"
                            esFalla = false
                            descripcionFalla = ""
                        }
                    }
                ) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoDeduccion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
