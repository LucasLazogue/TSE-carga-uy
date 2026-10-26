package uy.tse.cargauy.ui.screens.viaje

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

import uy.tse.cargauy.data.DESCRIPCION_MAXIMA
import uy.tse.cargauy.data.TipoEvento
import uy.tse.cargauy.data.errorDelEvento
import uy.tse.cargauy.data.kilosConUnidad
import uy.tse.cargauy.data.llevaKilos
import uy.tse.cargauy.ui.components.BotonPrincipal
import uy.tse.cargauy.ui.components.BotonTexto
import uy.tse.cargauy.ui.components.Descripcion
import uy.tse.cargauy.ui.components.TextoError
import uy.tse.cargauy.ui.theme.Spacing

private fun tituloDelFormulario(tipo: TipoEvento) = when (tipo) {
    TipoEvento.CARGA_PARCIAL -> "Carga parcial"
    TipoEvento.DESCARGA_PARCIAL -> "Descarga parcial"
    else -> "Reportar incidente"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioEvento(
    tipo: TipoEvento,
    volumen: Int,
    onGuardar: (descripcion: String, kilos: Int?) -> Unit,
    onCancelar: () -> Unit
) {
    var kilosTexto by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    // los errores aparecen recien al intentar guardar, no mientras escribe
    var intento by rememberSaveable { mutableStateOf(false) }
    val kilos = kilosTexto.toIntOrNull()
    val error = errorDelEvento(tipo, kilos, descripcion, volumen)
    val incidente = tipo == TipoEvento.INCIDENTE

    ModalBottomSheet(onDismissRequest = onCancelar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(text = tituloDelFormulario(tipo), style = MaterialTheme.typography.titleLarge)
            Descripcion(texto = "Se guarda con la hora actual y se envía al central cuando haya señal.")

            if (llevaKilos(tipo)) {
                OutlinedTextField(
                    value = kilosTexto,
                    // solo digitos: el teclado numerico deja escribir puntos y comas
                    onValueChange = { nuevo -> kilosTexto = nuevo.filter(Char::isDigit).take(7) },
                    label = { Text("Kilos") },
                    suffix = { Text("kg") },
                    supportingText = { Text("Hasta ${kilosConUnidad(volumen)}, el volumen de la guía.") },
                    isError = intento && error != null && !incidente,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = descripcion,
                onValueChange = { nuevo -> descripcion = nuevo.take(DESCRIPCION_MAXIMA) },
                label = { Text(if (incidente) "Qué pasó" else "Observación (opcional)") },
                placeholder = {
                    Text(if (incidente) "Ej.: pinchadura en ruta 5, km 210. Demora de 40 min." else "Ej.: entregado en el depósito de Salto")
                },
                supportingText = { Text("${descripcion.length} / $DESCRIPCION_MAXIMA") },
                isError = intento && error != null && incidente,
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            if (intento) error?.let { TextoError(texto = it) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End)) {
                BotonTexto(texto = "Cancelar", onClick = onCancelar)
                BotonPrincipal(
                    texto = "Guardar",
                    onClick = { if (error == null) onGuardar(descripcion, kilos) else intento = true }
                )
            }
        }
    }
}
