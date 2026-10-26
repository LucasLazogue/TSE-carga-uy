package uy.tse.cargauy.data

import java.time.Instant

import uy.tse.cargauy.data.local.EventoPendiente
import uy.tse.cargauy.data.local.ViajeLocal
import uy.tse.cargauy.data.remote.EventoSyncDto
import uy.tse.cargauy.data.remote.ViajeMovilDto

// los mismos nombres que los enums del backend (TipoEventoViaje y EstadoViaje): viajan como texto en el json
enum class TipoEvento { INICIO, CARGA_PARCIAL, DESCARGA_PARCIAL, INCIDENTE, FIN }

enum class EstadoViaje { ASIGNADO, EN_CURSO, FINALIZADO }

// el estado que se muestra sin esperar al central; null si no corresponde (igual se envia y decide el central)
fun estadoTras(actual: String, tipo: TipoEvento): String? = when (tipo) {
    TipoEvento.INICIO -> if (actual == EstadoViaje.ASIGNADO.name) EstadoViaje.EN_CURSO.name else null
    TipoEvento.CARGA_PARCIAL, TipoEvento.DESCARGA_PARCIAL, TipoEvento.INCIDENTE ->
        if (actual == EstadoViaje.EN_CURSO.name) actual else null
    TipoEvento.FIN -> if (actual == EstadoViaje.EN_CURSO.name) EstadoViaje.FINALIZADO.name else null
}

// los campos que pueden faltar en el json quedan con un valor por defecto: en Room son obligatorios
fun aViajeLocal(dto: ViajeMovilDto, idUsuario: Long) = ViajeLocal(
    idViaje = dto.viaje.id,
    idUsuario = idUsuario,
    estado = dto.viaje.estado,
    nroGuia = dto.guia.nroGuia ?: "",
    fechaGuia = dto.guia.fecha ?: "",
    matricula = dto.viaje.matricula ?: "",
    origenLat = dto.guia.origenLat ?: 0.0,
    origenLon = dto.guia.origenLon ?: 0.0,
    destinoLat = dto.guia.destinoLat ?: 0.0,
    destinoLon = dto.guia.destinoLon ?: 0.0,
    volumen = dto.guia.volumen,
    rubro = dto.guia.nombreRubro ?: "",
    tipoCarga = dto.guia.nombreTipoCarga ?: "",
    fechaInicio = dto.viaje.fechaInicio
)

// la hora viaja en ISO-8601 UTC (2026-10-11T14:03:22.123Z), que es lo que el backend lee como Instant
fun aEventoDto(evento: EventoPendiente) = EventoSyncDto(
    idEvento = evento.idEvento,
    idViaje = evento.idViaje,
    tipo = evento.tipo,
    timestampGeneracion = Instant.ofEpochMilli(evento.timestampGeneracion).toString(),
    descripcion = evento.descripcion,
    kilos = evento.kilos
)
