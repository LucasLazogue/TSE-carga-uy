package uy.tse.cargauy.ui.screens.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import uy.tse.cargauy.data.TipoEvento
import uy.tse.cargauy.data.local.EventoPendiente
import uy.tse.cargauy.data.local.ViajeLocal
import uy.tse.cargauy.ui.UiState
import uy.tse.cargauy.ui.components.Aviso
import uy.tse.cargauy.ui.components.Contenido
import uy.tse.cargauy.ui.components.Descripcion
import uy.tse.cargauy.ui.components.TextoError
import uy.tse.cargauy.ui.components.TituloSeccion
import uy.tse.cargauy.ui.theme.Spacing

// pantalla provisoria para probar el registro y el envio de eventos; la reemplaza la pantalla del viaje (B8)
@Composable
fun InicioScreen(
    viewModel: InicioViewModel = viewModel()
) {
    val viaje by viewModel.viaje.collectAsStateWithLifecycle()
    val pendientes by viewModel.pendientes.collectAsStateWithLifecycle()
    val enviados by viewModel.enviados.collectAsStateWithLifecycle()
    val conRed by viewModel.conRed.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (!conRed) {
            Aviso(texto = "Sin conexión. Los eventos se guardan en el celular y se envían solos cuando vuelva la señal.")
        } else {
            error?.let { TextoError(texto = it) }
        }

        when (val actual = viaje) {
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Success -> {
                val datos = actual.data
                if (datos == null) {
                    SinViaje(onActualizar = viewModel::actualizar)
                } else {
                    DatosViaje(datos)
                    Eventos(onRegistrar = viewModel::registrar)
                }
            }
            is UiState.Error -> TextoError(texto = actual.mensaje)
        }

        Contenido(texto = "Eventos sin enviar: $pendientes")

        if (enviados.isNotEmpty()) {
            Enviados(enviados = enviados, onLimpiar = viewModel::borrarEnviados)
        }
    }
}

@Composable
private fun SinViaje(onActualizar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TituloSeccion(texto = "No tenés viajes asignados")
        Descripcion(texto = "Cuando te asignen uno, aparece acá.")
        OutlinedButton(onClick = onActualizar) {
            Text("Actualizar")
        }
    }
}

@Composable
private fun DatosViaje(viaje: ViajeLocal) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        TituloSeccion(texto = "Viaje ${viaje.idViaje} · Guía ${viaje.nroGuia}")
        Descripcion(texto = "Fecha ${viaje.fechaGuia} · Matrícula ${viaje.matricula}")
        Descripcion(texto = "${viaje.rubro} · ${viaje.tipoCarga} · ${viaje.volumen} kg")
        Contenido(texto = "Estado: ${viaje.estado}")
    }
}

@Composable
private fun Eventos(onRegistrar: (TipoEvento) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TituloSeccion(texto = "Generar eventos")
        Descripcion(texto = "Cada botón registra un evento con la hora actual. El central decide si lo aplica.")
        // dos botones por fila
        TipoEvento.entries.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                fila.forEach { tipo ->
                    OutlinedButton(onClick = { onRegistrar(tipo) }, modifier = Modifier.weight(1f)) {
                        Text(etiqueta(tipo.name))
                    }
                }
                // la ultima fila tiene un solo boton: el espacio mantiene el mismo ancho
                if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Enviados(enviados: List<EventoPendiente>, onLimpiar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TituloSeccion(texto = "Enviados")
            TextButton(onClick = onLimpiar) {
                Text("Limpiar")
            }
        }
        enviados.forEach { evento ->
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Contenido(texto = "${etiqueta(evento.tipo)} · ${hora(evento.timestampGeneracion)}")
                Resultado(resultado = evento.resultado ?: "")
            }
            evento.mensaje?.let { Descripcion(texto = it) }
        }
    }
}

// verde (primary) si el central lo tomo; rojo (error) si no lo aplico
@Composable
private fun Resultado(resultado: String) {
    val aplicado = resultado == "APLICADO" || resultado == "DUPLICADO"
    Text(
        text = resultado,
        style = MaterialTheme.typography.labelLarge,
        color = if (aplicado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    )
}

private fun etiqueta(tipo: String): String = when (tipo) {
    TipoEvento.INICIO.name -> "Inicio"
    TipoEvento.CARGA_PARCIAL.name -> "Carga parcial"
    TipoEvento.DESCARGA_PARCIAL.name -> "Descarga parcial"
    TipoEvento.INCIDENTE.name -> "Incidente"
    TipoEvento.FIN.name -> "Fin"
    else -> tipo
}

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

private fun hora(milisegundos: Long): String = formatoHora.format(Instant.ofEpochMilli(milisegundos))
