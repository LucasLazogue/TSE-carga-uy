package uy.tse.cargauy.ui.screens.viaje

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

import uy.tse.cargauy.data.Acciones
import uy.tse.cargauy.data.TipoEvento
import uy.tse.cargauy.data.accionesPara
import uy.tse.cargauy.data.coordenadas
import uy.tse.cargauy.data.fechaHoraLegible
import uy.tse.cargauy.data.fechaLegible
import uy.tse.cargauy.data.kilosConUnidad
import uy.tse.cargauy.data.local.EventoPendiente
import uy.tse.cargauy.data.local.ViajeLocal
import uy.tse.cargauy.ui.UiState
import uy.tse.cargauy.ui.components.Aviso
import uy.tse.cargauy.ui.components.BotonPeligro
import uy.tse.cargauy.ui.components.BotonPrincipal
import uy.tse.cargauy.ui.components.BotonSecundario
import uy.tse.cargauy.ui.components.BotonTexto
import uy.tse.cargauy.ui.components.Cargando
import uy.tse.cargauy.ui.components.Confirmacion
import uy.tse.cargauy.ui.components.ContenidoPantalla
import uy.tse.cargauy.ui.components.DatoFila
import uy.tse.cargauy.ui.components.Descripcion
import uy.tse.cargauy.ui.components.EstadoVacio
import uy.tse.cargauy.ui.components.EstadoViajeChip
import uy.tse.cargauy.ui.components.FilaEvento
import uy.tse.cargauy.ui.components.Seccion
import uy.tse.cargauy.ui.components.Tarjeta
import uy.tse.cargauy.ui.theme.Sizes
import uy.tse.cargauy.ui.theme.Spacing

@Composable
fun ViajeScreen(
    onVolver: () -> Unit,
    viewModel: ViajeViewModel = viewModel()
) {
    val viaje by viewModel.viaje.collectAsStateWithLifecycle()
    val hayOtroEnCurso by viewModel.hayOtroEnCurso.collectAsStateWithLifecycle()
    val eventos by viewModel.eventos.collectAsStateWithLifecycle()
    val conRed by viewModel.conRed.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var formulario by rememberSaveable { mutableStateOf<TipoEvento?>(null) }
    var confirmarFin by rememberSaveable { mutableStateOf(false) }

    fun avisar(texto: String) {
        scope.launch { avisos.showSnackbar(texto) }
    }

    fun guardado(tipo: TipoEvento) =
        avisar("${nombreDeLaAccion(tipo)}: guardado." + if (conRed) " Enviando al central." else " Se envía cuando haya señal.")

    Box(modifier = Modifier.fillMaxSize()) {
        when (val actual = viaje) {
            is UiState.Loading -> Cargando()
            is UiState.Error -> EstadoVacio(titulo = "No se pudo leer el viaje", descripcion = actual.mensaje)
            is UiState.Success -> {
                val datos = actual.data
                if (datos == null) {
                    ViajeNoDisponible(onVolver = onVolver)
                } else {
                    ContenidoPantalla {
                        Encabezado(viaje = datos)
                        if (!conRed) Aviso(texto = "Sin conexión. Lo que registres se envía solo cuando vuelva la señal.")
                        BotonesDelViaje(
                            acciones = accionesPara(datos, hayOtroEnCurso, LocalDate.now()),
                            onIniciar = {
                                viewModel.registrar(TipoEvento.INICIO)
                                guardado(TipoEvento.INICIO)
                            },
                            onFormulario = { formulario = it },
                            onFinalizar = { confirmarFin = true }
                        )
                        Recorrido(viaje = datos, onMapa = { latitud, longitud, nombre ->
                            if (!abrirEnMapa(context, latitud, longitud, nombre)) avisar("No hay una app de mapas instalada.")
                        })
                        Carga(viaje = datos)
                        Historial(eventos = eventos)
                    }

                    formulario?.let { tipo ->
                        FormularioEvento(
                            tipo = tipo,
                            volumen = datos.volumen,
                            onGuardar = { descripcion, kilos ->
                                viewModel.registrar(tipo, descripcion, kilos)
                                formulario = null
                                guardado(tipo)
                            },
                            onCancelar = { formulario = null }
                        )
                    }

                    if (confirmarFin) {
                        Confirmacion(
                            titulo = "¿Finalizar el viaje?",
                            texto = "Guía ${datos.nroGuia}. Después de finalizarlo no vas a poder registrar más eventos en este viaje.",
                            textoConfirmar = "Finalizar",
                            textoCancelar = "Seguir en viaje",
                            onConfirmar = {
                                confirmarFin = false
                                viewModel.registrar(TipoEvento.FIN)
                                guardado(TipoEvento.FIN)
                            },
                            onCancelar = { confirmarFin = false }
                        )
                    }
                }
            }
        }
        SnackbarHost(hostState = avisos, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

private fun nombreDeLaAccion(tipo: TipoEvento) = when (tipo) {
    TipoEvento.INICIO -> "Inicio del viaje"
    TipoEvento.CARGA_PARCIAL -> "Carga parcial"
    TipoEvento.DESCARGA_PARCIAL -> "Descarga parcial"
    TipoEvento.INCIDENTE -> "Incidente"
    TipoEvento.FIN -> "Fin del viaje"
}

@Composable
private fun Encabezado(viaje: ViajeLocal) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = "Guía ${viaje.nroGuia}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            EstadoViajeChip(estado = viaje.estado)
        }
        Descripcion(texto = "Del ${fechaLegible(viaje.fechaGuia)} · ${viaje.matricula}")
        viaje.fechaInicio?.let { Descripcion(texto = "Inició el ${fechaHoraLegible(it)}") }
    }
}

@Composable
private fun BotonesDelViaje(
    acciones: Acciones,
    onIniciar: () -> Unit,
    onFormulario: (TipoEvento) -> Unit,
    onFinalizar: () -> Unit
) {
    val eventos = acciones.eventos
    if (eventos.isEmpty()) {
        Aviso(texto = "El viaje está finalizado. Se quita de tu lista cuando se sincronice.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (TipoEvento.INICIO in eventos) {
            BotonPrincipal(
                texto = "Iniciar viaje",
                onClick = onIniciar,
                enabled = acciones.bloqueo == null,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (TipoEvento.CARGA_PARCIAL in eventos) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BotonSecundario(
                    texto = "Carga parcial",
                    onClick = { onFormulario(TipoEvento.CARGA_PARCIAL) },
                    modifier = Modifier.weight(1f)
                )
                BotonSecundario(
                    texto = "Descarga parcial",
                    onClick = { onFormulario(TipoEvento.DESCARGA_PARCIAL) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (TipoEvento.INCIDENTE in eventos) {
            BotonSecundario(
                texto = "Reportar incidente",
                onClick = { onFormulario(TipoEvento.INCIDENTE) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (TipoEvento.FIN in eventos) {
            BotonPeligro(texto = "Finalizar viaje", onClick = onFinalizar, modifier = Modifier.fillMaxWidth())
        }
        acciones.bloqueo?.let { Descripcion(texto = it) }
    }
}

@Composable
private fun Recorrido(viaje: ViajeLocal, onMapa: (Double, Double, String) -> Unit) {
    Seccion(titulo = "Recorrido") {
        Tarjeta {
            Lugar(nombre = "Origen", latitud = viaje.origenLat, longitud = viaje.origenLon, onMapa = onMapa)
            HorizontalDivider()
            Lugar(nombre = "Destino", latitud = viaje.destinoLat, longitud = viaje.destinoLon, onMapa = onMapa)
        }
    }
}

@Composable
private fun Lugar(nombre: String, latitud: Double, longitud: Double, onMapa: (Double, Double, String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Sizes.icono)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = nombre, style = MaterialTheme.typography.titleSmall)
            Descripcion(texto = coordenadas(latitud, longitud))
        }
        if (coordenadasValidas(latitud, longitud)) {
            BotonTexto(texto = "Ver en el mapa", onClick = { onMapa(latitud, longitud, nombre) })
        }
    }
}

@Composable
private fun Carga(viaje: ViajeLocal) {
    Seccion(titulo = "Carga") {
        Tarjeta {
            DatoFila(etiqueta = "Rubro", valor = viaje.rubro)
            DatoFila(etiqueta = "Tipo de carga", valor = viaje.tipoCarga)
            DatoFila(etiqueta = "Volumen", valor = kilosConUnidad(viaje.volumen))
            DatoFila(etiqueta = "Vehículo", valor = viaje.matricula)
        }
    }
}

@Composable
private fun Historial(eventos: List<EventoPendiente>) {
    Seccion(titulo = "Eventos") {
        if (eventos.isEmpty()) {
            Descripcion(texto = "Todavía no registraste eventos en este viaje.")
        } else {
            Tarjeta {
                eventos.forEachIndexed { indice, evento ->
                    if (indice > 0) HorizontalDivider()
                    FilaEvento(evento = evento)
                }
            }
        }
    }
}

@Composable
private fun ViajeNoDisponible(onVolver: () -> Unit) {
    EstadoVacio(
        titulo = "Este viaje ya no está en tu lista",
        descripcion = "Terminó, se reasignó o se eliminó desde la web. Si algún evento tuyo no se aplicó, lo ves en Mis viajes.",
        modifier = Modifier.padding(Spacing.md)
    ) {
        BotonPrincipal(texto = "Volver a mis viajes", onClick = onVolver)
    }
}

private fun coordenadasValidas(latitud: Double, longitud: Double) =
    latitud in -90.0..90.0 && longitud in -180.0..180.0 && !(latitud == 0.0 && longitud == 0.0)

private fun abrirEnMapa(context: Context, latitud: Double, longitud: Double, nombre: String): Boolean {
    val uri = "geo:$latitud,$longitud?q=$latitud,$longitud(${Uri.encode(nombre)})".toUri()
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
