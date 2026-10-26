package tse.cargauy.data.pesaje;

import tse.cargauy.dtos.PesajeDto;
import tse.cargauy.entities.Pesaje;

public class Serializers {
    public static Pesaje toEntity(PesajeDto dto) {
        Pesaje pesaje = new Pesaje();
        pesaje.setIdOrigen(dto.getIdOrigen());
        pesaje.setIdNodo(dto.getIdNodo());
        pesaje.setIdVehiculo(dto.getIdVehiculo());
        pesaje.setIdViaje(dto.getIdViaje());
        pesaje.setMatricula(dto.getMatricula());
        pesaje.setFechaHora(dto.getFechaHora());
        pesaje.setPesoRegistrado(dto.getPesoRegistrado());
        pesaje.setCodigoBalanza(dto.getCodigoBalanza());
        pesaje.setSentido(dto.getSentido());
        pesaje.setCodigoPeaje(dto.getCodigoPeaje());
        pesaje.setNombrePeaje(dto.getNombrePeaje());
        pesaje.setRuta(dto.getRuta());
        pesaje.setKm(dto.getKm());
        pesaje.setLatitud(dto.getLatitud());
        pesaje.setLongitud(dto.getLongitud());
        pesaje.setRecibidoEn(dto.getRecibidoEn());
        return pesaje;
    }

    public static PesajeDto toDto(Pesaje pesaje) {
        PesajeDto dto = new PesajeDto();
        dto.setIdOrigen(pesaje.getIdOrigen());
        dto.setIdNodo(pesaje.getIdNodo());
        dto.setIdVehiculo(pesaje.getIdVehiculo());
        dto.setIdViaje(pesaje.getIdViaje());
        dto.setMatricula(pesaje.getMatricula());
        dto.setFechaHora(pesaje.getFechaHora());
        dto.setPesoRegistrado(pesaje.getPesoRegistrado());
        dto.setCodigoBalanza(pesaje.getCodigoBalanza());
        dto.setSentido(pesaje.getSentido());
        dto.setCodigoPeaje(pesaje.getCodigoPeaje());
        dto.setNombrePeaje(pesaje.getNombrePeaje());
        dto.setRuta(pesaje.getRuta());
        dto.setKm(pesaje.getKm());
        dto.setLatitud(pesaje.getLatitud());
        dto.setLongitud(pesaje.getLongitud());
        dto.setRecibidoEn(pesaje.getRecibidoEn());
        return dto;
    }
}
