package uy.tse.cargauy.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import uy.tse.cargauy.ui.theme.Sizes

// una pantalla tiene como mucho un BotonPrincipal a la vista
@Composable
fun BotonPrincipal(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = Sizes.tactil)) {
        Text(texto)
    }
}

@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = Sizes.tactil)) {
        Text(texto)
    }
}

@Composable
fun BotonPeligro(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        modifier = modifier.heightIn(min = Sizes.tactil)
    ) {
        Text(texto)
    }
}

@Composable
fun BotonTexto(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, peligro: Boolean = false) {
    TextButton(
        onClick = onClick,
        colors = if (peligro) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        else ButtonDefaults.textButtonColors(),
        modifier = modifier.heightIn(min = Sizes.tactil)
    ) {
        Text(texto)
    }
}
