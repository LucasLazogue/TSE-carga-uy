package uy.tse.cargauy.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import uy.tse.cargauy.ui.theme.ParDeColores
import uy.tse.cargauy.ui.theme.Radius
import uy.tse.cargauy.ui.theme.Spacing

@Composable
fun Chip(texto: String, colores: ParDeColores, modifier: Modifier = Modifier) {
    Surface(
        color = colores.fondo,
        contentColor = colores.contenido,
        shape = RoundedCornerShape(Radius.full),
        modifier = modifier
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
        )
    }
}
