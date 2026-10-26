package uy.tse.cargauy.ui.screens.viajes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

import uy.tse.cargauy.data.EstadoViaje
import uy.tse.cargauy.data.accionesPara
import uy.tse.cargauy.data.fechaHoraLegible
import uy.tse.cargauy.data.fechaLegible
import uy.tse.cargauy.data.kilosConUnidad
import uy.tse.cargauy.data.local.EventoPendiente
import uy.tse.cargauy.data.local.ViajeLocal
import uy.tse.cargauy.ui.UiState
import uy.tse.cargauy.ui.components.Aviso
import uy.tse.cargauy.ui.components.BotonSecundario
import uy.tse.cargauy.ui.components.BotonTexto
import uy.tse.cargauy.ui.components.Cargando
import uy.tse.cargauy.ui.components.Descripcion
import uy.tse.cargauy.ui.components.EstadoVacio
import uy.tse.cargauy.ui.components.EstadoViajeChip
import uy.tse.cargauy.ui.components.FilaEvento
import uy.tse.cargauy.ui.components.Tarjeta
import uy.tse.cargauy.ui.components.TextoError
import uy.tse.cargauy.ui.components.TituloSeccion
import uy.tse.cargauy.ui.theme.Spacing
import uy.tse.cargauy.ui.theme.coloresSemanticos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViajesScreen(
    onAbrirViaje: (Long) -> Unit,
    viewModel: ViajesViewModel = viewModel()
) {
    val viajes by viewModel.viajes.collectAsStateWithLifecycle()
    val pendientes by viewModel.pendientes.collectAsStateWithLifecycle()
    val noAplicados by viewModel.noAplicados.collectAsStateWithLifecycle()
    val conRed by viewModel.conRed.collectAsStateWithLifecycle()
    val actualizando by viewModel.actualizando.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = actualizando,
        onRefresh = viewModel::actualizar,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item { EstadoDeConexion(conRed = conRed, pendientes = pendientes, error = error) }

            when (val actual = viajes) {
                is UiState.Loading -> item { Cargando() }
                is UiState.Error -> item { TextoError(texto = actual.mensaje) }
                is UiState.Success -> {
                    val lista = actual.data
                    if (lista.isEmpty()) {
                        item {
                            EstadoVacio(
                                titulo = "No tenés viajes asignados",
                                descripcion = "Cuando te asignen uno, aparece acá. Deslizá hacia abajo para actualizar."
                            ) {
                                BotonSecundario(texto = "Actualizar", onClick = viewModel::actualizar)
                            }
                        }
                    }
                    val hoy = LocalDate.now()
                    grupos(lista).forEach { (titulo, delGrupo) ->
                        item(key = "titulo-$titulo") { TituloSeccion(texto = titulo) }
                        items(delGrupo, key = { it.idViaje }) { viaje ->
                            val otroEnCurso = lista.any { it.idViaje != viaje.idViaje && it.estado == EstadoViaje.EN_CURSO.name }
                            TarjetaViaje(
                                viaje = viaje,
                                bloqueo = accionesPara(viaje, otroEnCurso, hoy).bloqueo,
                                onClick = { onAbrirViaje(viaje.idViaje) }
                            )
                        }
                    }
                }
            }

            if (noAplicados.isNotEmpty()) {
                item { NoAplicados(eventos = noAplicados, onEntendido = viewModel::descartarNoAplicados) }
            }
        }
    }
}

// un viaje recien finalizado queda hasta que se sincroniza
private fun grupos(viajes: List<ViajeLocal>): List<Pair<String, List<ViajeLocal>>> = listOf(
    "En curso" to viajes.filter { it.estado == EstadoViaje.EN_CURSO.name },
    "Asignados" to viajes.filter { it.estado == EstadoViaje.ASIGNADO.name },
    "Finalizados" to viajes.filter { it.estado == EstadoViaje.FINALIZADO.name }
).filter { it.second.isNotEmpty() }

@Composable
private fun EstadoDeConexion(conRed: Boolean, pendientes: Int, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (!conRed) {
            Aviso(texto = "Sin conexión. Podés seguir registrando eventos: se envían solos cuando vuelva la señal.")
        } else {
            error?.let { TextoError(texto = it) }
        }
        if (pendientes > 0) {
            Aviso(
                texto = if (pendientes == 1) "1 evento sin enviar." else "$pendientes eventos sin enviar.",
                colores = MaterialTheme.coloresSemanticos.aviso
            )
        }
    }
}

@Composable
private fun TarjetaViaje(viaje: ViajeLocal, bloqueo: String?, onClick: () -> Unit) {
    Tarjeta(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(text = "Guía ${viaje.nroGuia}", style = MaterialTheme.typography.titleMedium)
                Descripcion(texto = "${fechaLegible(viaje.fechaGuia)} · ${viaje.matricula}")
            }
            EstadoViajeChip(estado = viaje.estado)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${viaje.rubro} · ${viaje.tipoCarga} · ${kilosConUnidad(viaje.volumen)}",
            style = MaterialTheme.typography.bodyMedium
        )
        viaje.fechaInicio?.let { Descripcion(texto = "Inició el ${fechaHoraLegible(it)}") }
        bloqueo?.let { Descripcion(texto = it) }
    }
}

@Composable
private fun NoAplicados(eventos: List<EventoPendiente>, onEntendido: () -> Unit) {
    Tarjeta {
        TituloSeccion(texto = "Eventos no aplicados")
        Descripcion(texto = "El central no aplicó estos eventos, y su viaje ya no está en tu lista. Quedaron registrados.")
        eventos.forEachIndexed { indice, evento ->
            if (indice > 0) HorizontalDivider()
            FilaEvento(evento = evento)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            BotonTexto(texto = "Entendido", onClick = onEntendido)
        }
    }
}
