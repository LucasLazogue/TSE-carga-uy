package uy.tse.cargauy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import uy.tse.cargauy.data.EstadoViaje
import uy.tse.cargauy.data.TipoEvento
import uy.tse.cargauy.data.detalleDelEvento
import uy.tse.cargauy.data.local.EventoPendiente
import uy.tse.cargauy.data.momentoDelEvento
import uy.tse.cargauy.ui.theme.Sizes
import uy.tse.cargauy.ui.theme.Spacing
import uy.tse.cargauy.ui.theme.coloresSemanticos

fun nombreDelEvento(tipo: String): String = when (tipo) {
    TipoEvento.INICIO.name -> "Inicio"
    TipoEvento.CARGA_PARCIAL.name -> "Carga parcial"
    TipoEvento.DESCARGA_PARCIAL.name -> "Descarga parcial"
    TipoEvento.INCIDENTE.name -> "Incidente"
    TipoEvento.FIN.name -> "Fin"
    else -> tipo
}

@Composable
fun EstadoViajeChip(estado: String, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.coloresSemanticos
    when (estado) {
        EstadoViaje.EN_CURSO.name -> Chip("En curso", colores.destacado, modifier)
        EstadoViaje.FINALIZADO.name -> Chip("Finalizado", colores.exito, modifier)
        else -> Chip("Asignado", colores.neutro, modifier)
    }
}

@Composable
fun ResultadoChip(resultado: String?, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.coloresSemanticos
    when (resultado) {
        null -> Chip("Sin enviar", colores.aviso, modifier)
        // un duplicado es un reenvio de algo que el central ya tenia: para el chofer, quedo registrado
        "APLICADO", "DUPLICADO" -> Chip("Aplicado", colores.exito, modifier)
        "CONFLICTO" -> Chip("Conflicto", colores.error, modifier)
        else -> Chip("Rechazado", colores.error, modifier)
    }
}

@Composable
fun FilaEvento(evento: EventoPendiente, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = momentoDelEvento(evento.timestampGeneracion),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = Sizes.columnaHora).alignByBaseline()
        )
        Column(
            modifier = Modifier.weight(1f).alignByBaseline(),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            Text(text = nombreDelEvento(evento.tipo), style = MaterialTheme.typography.titleSmall)
            detalleDelEvento(evento.kilos, evento.descripcion)?.let { Descripcion(texto = it) }
            if (evento.resultado == "CONFLICTO" || evento.resultado == "RECHAZADO") {
                evento.mensaje?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        ResultadoChip(resultado = evento.resultado)
    }
}
