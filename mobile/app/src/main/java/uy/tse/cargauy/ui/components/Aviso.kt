package uy.tse.cargauy.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import uy.tse.cargauy.ui.theme.ParDeColores
import uy.tse.cargauy.ui.theme.Spacing
import uy.tse.cargauy.ui.theme.coloresSemanticos

/** Aviso destacado que no es un error: algo que el usuario tiene que saber sobre lo que va a pasar. */
@Composable
fun Aviso(
    texto: String,
    modifier: Modifier = Modifier,
    colores: ParDeColores = MaterialTheme.coloresSemanticos.info
) {
    Surface(
        color = colores.fondo,
        contentColor = colores.contenido,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
        )
    }
}
