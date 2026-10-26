package uy.tse.cargauy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

import uy.tse.cargauy.ui.theme.FRACCION_MENU_LATERAL
import uy.tse.cargauy.ui.theme.Spacing

data class OpcionMenu(
    val texto: String,
    val icono: ImageVector,
    val seleccionada: Boolean,
    val onClick: () -> Unit
)

@Composable
fun MenuLateral(
    cuenta: List<String>,
    opciones: List<OpcionMenu>,
    onCerrarSesion: () -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(FRACCION_MENU_LATERAL)) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = "Sesión iniciada",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            cuenta.forEachIndexed { indice, linea ->
                if (indice == 0) TituloSeccion(texto = linea) else Descripcion(texto = linea)
            }
        }
        opciones.forEach { opcion ->
            NavigationDrawerItem(
                label = { Text(opcion.texto) },
                icon = { Icon(imageVector = opcion.icono, contentDescription = null) },
                selected = opcion.seleccionada,
                onClick = opcion.onClick,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text("Cerrar sesión") },
            icon = { Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
            selected = false,
            onClick = onCerrarSesion,
            modifier = Modifier
                .fillMaxWidth()
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .padding(bottom = Spacing.md)
        )
    }
}
