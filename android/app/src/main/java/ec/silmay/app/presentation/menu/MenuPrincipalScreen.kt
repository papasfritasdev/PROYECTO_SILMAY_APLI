package ec.silmay.app.presentation.menu

import androidx.annotation.StringRes
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ec.silmay.app.R
import ec.silmay.app.domain.model.Rol
import ec.silmay.app.domain.model.UsuarioSesion

/** Módulos operativos visibles según el rol de campo (architecture.md §2: M1, M2, M3). */
internal fun modulosPorRol(rol: Rol): List<Int> = buildList {
    add(R.string.menu_modulo_jornada)
    add(R.string.menu_modulo_despachos)
    add(R.string.menu_modulo_gastos)
    if (rol == Rol.SUPERVISOR) add(R.string.menu_modulo_registros_zona)
}

@StringRes
private fun tituloPorRol(rol: Rol): Int = when (rol) {
    Rol.SUPERVISOR -> R.string.menu_titulo_supervisor
    else -> R.string.menu_titulo_operador
}

/**
 * Menú principal por rol tras el inicio de sesión (HU-01a CA1).
 * Los módulos se habilitarán en el Sprint 3 (HU-03, HU-04) y Sprint 4 (HU-06).
 */
@Composable
fun MenuPrincipalScreen(
    sesion: UsuarioSesion,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    alertaPendientes: Int? = null,
    onDescartarAlertaPendientes: () -> Unit = {},
    onModuloClick: (Int) -> Unit = {},
    esOnline: Boolean = true,
    pendientesSync: Int = 0,
    onForzarSincronizacion: () -> Unit = {}
) {
    if (alertaPendientes != null) {
        AlertDialog(
            onDismissRequest = onDescartarAlertaPendientes,
            title = {
                Text(text = stringResource(R.string.alerta_pendientes_titulo))
            },
            text = {
                Text(text = stringResource(R.string.alerta_pendientes_mensaje, alertaPendientes))
            },
            confirmButton = {
                TextButton(onClick = onDescartarAlertaPendientes) {
                    Text(text = stringResource(R.string.alerta_pendientes_boton))
                }
            }
        )
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(tituloPorRol(sesion.rol)),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = sesion.nombres, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.menu_zona, sesion.zonaAsignada),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Indicadores reactivos de conectividad y sincronización (HU-08 CA1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (esOnline) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = if (esOnline) stringResource(R.string.sync_estado_online)
                        else stringResource(R.string.sync_estado_offline),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (esOnline) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Card(
                    onClick = onForzarSincronizacion,
                    enabled = esOnline && pendientesSync > 0,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pendientesSync == 0) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = if (pendientesSync == 0) stringResource(R.string.sync_todo_sincronizado)
                        else stringResource(R.string.sync_pendientes_formato, pendientesSync),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pendientesSync == 0) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            modulosPorRol(sesion.rol).forEach { modulo ->
                val esHabilitado = modulo == R.string.menu_modulo_jornada || modulo == R.string.menu_modulo_despachos
                Card(
                    onClick = { if (esHabilitado) onModuloClick(modulo) },
                    enabled = esHabilitado,
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (esHabilitado) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(modulo),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (esHabilitado) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = if (esHabilitado) "Toca para ingresar al módulo"
                            else stringResource(R.string.menu_modulo_proximamente),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCerrarSesion,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.menu_cerrar_sesion))
            }
        }
    }
}
