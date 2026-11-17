package ec.silmay.app.presentation.despacho

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ec.silmay.app.R
import ec.silmay.app.domain.model.MetodoCobro
import ec.silmay.app.domain.model.UnidadMedida

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DespachoScreen(
    viewModel: DespachoViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    var expandoMateriales by remember { mutableStateOf(false) }
    var expandoClientes by remember { mutableStateOf(false) }
    var expandoVolquetas by remember { mutableStateOf(false) }
    var expandoLugares by remember { mutableStateOf(false) }

    val mensajeExito = stringResource(R.string.despacho_guardado_exitoso)
    LaunchedEffect(state.operacionExitosa) {
        if (state.operacionExitosa) {
            snackbarHostState.showSnackbar(mensajeExito)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.despacho_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.despacho_volver)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }

            // Alerta de error del catálogo (MSG-04, MSG-07, MSG-18)
            state.errorMensaje?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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

            // Error de campos incompletos (CA2 / RF-12)
            if (state.errorCamposIncompletos && state.errorMensaje == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.despacho_error_campos),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Indicador de frente de extracción heredado de la jornada activa (HU-03 CA1)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.despacho_lugar),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = state.jornadaActiva?.let { "Frente: ${it.lugarId} (Heredado de la jornada activa)" }
                            ?: (state.lugarSeleccionado?.nombre ?: "Sin frente asignado"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Selector de Material
            Text(
                text = stringResource(R.string.despacho_material),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ExposedDropdownMenuBox(
                expanded = expandoMateriales,
                onExpandedChange = { expandoMateriales = !expandoMateriales },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.materialSeleccionado?.nombre ?: "Seleccionar material",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandoMateriales) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandoMateriales,
                    onDismissRequest = { expandoMateriales = false }
                ) {
                    state.materiales.forEach { mat ->
                        DropdownMenuItem(
                            text = { Text(mat.nombre) },
                            onClick = {
                                viewModel.alSeleccionarMaterial(mat)
                                expandoMateriales = false
                            }
                        )
                    }
                }
            }

            // Unidad de Medida (m3 / viajes)
            Text(
                text = stringResource(R.string.despacho_unidad),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UnidadMedida.entries.forEach { unidad ->
                    FilterChip(
                        selected = state.unidadMedida == unidad,
                        onClick = { viewModel.alSeleccionarUnidad(unidad) },
                        label = { Text(unidad.valor.uppercase()) }
                    )
                }
            }

            // Cantidad
            Text(
                text = stringResource(R.string.despacho_cantidad),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = state.cantidad,
                onValueChange = viewModel::alCambiarCantidad,
                label = { Text("Cantidad a despachar (> 0)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                isError = state.errorCamposIncompletos && state.cantidad.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            // Selector de Cliente
            Text(
                text = stringResource(R.string.despacho_cliente),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ExposedDropdownMenuBox(
                expanded = expandoClientes,
                onExpandedChange = { expandoClientes = !expandoClientes },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.clienteSeleccionado?.nombre ?: "Seleccionar cliente",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandoClientes) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandoClientes,
                    onDismissRequest = { expandoClientes = false }
                ) {
                    state.clientes.forEach { cli ->
                        DropdownMenuItem(
                            text = { Text(cli.nombre) },
                            onClick = {
                                viewModel.alSeleccionarCliente(cli)
                                expandoClientes = false
                            }
                        )
                    }
                }
            }

            // Selector de Volqueta
            Text(
                text = stringResource(R.string.despacho_volqueta),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ExposedDropdownMenuBox(
                expanded = expandoVolquetas,
                onExpandedChange = { expandoVolquetas = !expandoVolquetas },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.volquetaSeleccionada?.let { "${it.codigoInterno} (${it.placa ?: it.modelo})" } ?: "Seleccionar volqueta",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandoVolquetas) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandoVolquetas,
                    onDismissRequest = { expandoVolquetas = false }
                ) {
                    state.volquetas.forEach { vol ->
                        DropdownMenuItem(
                            text = { Text("${vol.codigoInterno} - Placa: ${vol.placa ?: "S/P"} (${vol.marca})") },
                            onClick = {
                                viewModel.alSeleccionarVolqueta(vol)
                                expandoVolquetas = false
                            }
                        )
                    }
                }
            }

            // Método de Cobro
            Text(
                text = stringResource(R.string.despacho_metodo_cobro),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetodoCobro.entries.forEach { metodo ->
                    FilterChip(
                        selected = state.metodoCobro == metodo,
                        onClick = { viewModel.alSeleccionarMetodoCobro(metodo) },
                        label = { Text(metodo.valor.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Valor USD
            Text(
                text = stringResource(R.string.despacho_valor_usd),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = state.valorUSD,
                onValueChange = viewModel::alCambiarValorUSD,
                label = { Text("Monto total facturado / pactado (> 0.00)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                isError = state.errorCamposIncompletos && state.valorUSD.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Guardar con retroalimentación háptica
            Button(
                onClick = {
                    viewModel.guardarDespacho(
                        alertaHaptica = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                },
                enabled = !state.cargando,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(R.string.despacho_btn_guardar),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
