package uy.tse.cargauy.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun Confirmacion(
    titulo: String,
    texto: String,
    textoConfirmar: String,
    textoCancelar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    peligro: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { BotonTexto(texto = textoConfirmar, onClick = onConfirmar, peligro = peligro) },
        dismissButton = { BotonTexto(texto = textoCancelar, onClick = onCancelar) }
    )
}
