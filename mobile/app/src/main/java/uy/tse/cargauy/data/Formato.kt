package uy.tse.cargauy.data

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val uruguay = Locale.forLanguageTag("es-UY")
private val fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val diaYHora = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private val hora = DateTimeFormatter.ofPattern("HH:mm")

fun kilosConUnidad(kilos: Int): String = NumberFormat.getIntegerInstance(uruguay).format(kilos) + " kg"

fun fechaLegible(iso: String): String = runCatching { LocalDate.parse(iso).format(fecha) }.getOrDefault(iso)

fun fechaHoraLegible(iso: String): String = runCatching { LocalDateTime.parse(iso).format(diaYHora) }.getOrDefault(iso)

fun momentoDelEvento(milisegundos: Long, zona: ZoneId = ZoneId.systemDefault(), hoy: LocalDate = LocalDate.now(zona)): String {
    val momento = Instant.ofEpochMilli(milisegundos).atZone(zona)
    return momento.format(if (momento.toLocalDate() == hoy) hora else diaYHora)
}

fun cedulaLegible(cedula: String): String {
    if (cedula.length < 2 || !cedula.all(Char::isDigit)) return cedula
    val numero = NumberFormat.getIntegerInstance(uruguay).format(cedula.dropLast(1).toLong())
    return "$numero-${cedula.last()}"
}

fun coordenadas(latitud: Double, longitud: Double): String = String.format(Locale.US, "%.4f, %.4f", latitud, longitud)

fun detalleDelEvento(kilos: Int?, descripcion: String?): String? =
    listOfNotNull(kilos?.let { kilosConUnidad(it) }, descripcion?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
        .ifEmpty { null }
