package uy.tse.cargauy.ui.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import uy.tse.cargauy.ui.theme.Spacing

@Composable
fun SettingsScreen(
    temaOscuro: Boolean,
    onCambiarTema: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tema oscuro",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Cambia los colores de la aplicación",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = temaOscuro,
                onCheckedChange = onCambiarTema
            )
        }
    }
}