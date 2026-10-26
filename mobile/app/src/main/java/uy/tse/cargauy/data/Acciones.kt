package uy.tse.cargauy.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import uy.tse.cargauy.data.local.ViajeLocal

// el mismo limite que EventoViaje.DESCRIPCION_MAXIMA en el central
const val DESCRIPCION_MAXIMA = 500

data class Acciones(val eventos: List<TipoEvento>, val bloqueo: String? = null)

private val formatoDia = DateTimeFormatter.ofPattern("dd/MM")

// lo que la app no puede saber (el vehiculo en un viaje de otro chofer) lo decide el central
fun accionesPara(viaje: ViajeLocal, hayOtroEnCurso: Boolean, hoy: LocalDate): Acciones = when (viaje.estado) {
    EstadoViaje.ASIGNADO.name -> {
        val desde = runCatching { LocalDate.parse(viaje.fechaGuia) }.getOrNull()
        when {
            hayOtroEnCurso -> Acciones(listOf(TipoEvento.INICIO), "Tenés otro viaje en curso. Finalizalo para iniciar este.")
            desde != null && hoy.isBefore(desde) ->
                Acciones(listOf(TipoEvento.INICIO), "Se puede iniciar desde el ${desde.format(formatoDia)}.")
            else -> Acciones(listOf(TipoEvento.INICIO))
        }
    }
    EstadoViaje.EN_CURSO.name -> Acciones(
        listOf(TipoEvento.CARGA_PARCIAL, TipoEvento.DESCARGA_PARCIAL, TipoEvento.INCIDENTE, TipoEvento.FIN)
    )
    else -> Acciones(emptyList())
}

fun llevaKilos(tipo: TipoEvento) = tipo == TipoEvento.CARGA_PARCIAL || tipo == TipoEvento.DESCARGA_PARCIAL

// las mismas validaciones que AplicadorEventosEJB
fun errorDelEvento(tipo: TipoEvento, kilos: Int?, descripcion: String, volumen: Int): String? = when {
    tipo == TipoEvento.INCIDENTE && descripcion.isBlank() -> "Describí qué pasó."
    descripcion.trim().length > DESCRIPCION_MAXIMA -> "La descripción puede tener hasta $DESCRIPCION_MAXIMA caracteres."
    !llevaKilos(tipo) -> null
    kilos == null -> "Indicá los kilos."
    kilos <= 0 -> "Los kilos tienen que ser más que 0."
    kilos > volumen -> "No puede superar el volumen de la guía (${kilosConUnidad(volumen)})."
    else -> null
}
