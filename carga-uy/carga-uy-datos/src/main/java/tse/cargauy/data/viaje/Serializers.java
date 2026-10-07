package tse.cargauy.data.viaje;

import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.Chofer;
import tse.cargauy.entities.Guia;
import tse.cargauy.entities.Vehiculo;
import tse.cargauy.entities.Viaje;

public class Serializers {
    public static ViajeDto toDto(Viaje viaje) {
        ViajeDto dto = new ViajeDto();
        dto.setId(viaje.getId());
        dto.setEstado(viaje.getEstado());
        dto.setFechaInicio(viaje.getFechaInicio());
        dto.setFechaFin(viaje.getFechaFin());
        dto.setIdGuia(viaje.getGuia().getId());
        dto.setNroGuia(viaje.getGuia().getNroGuia());
        dto.setIdVehiculo(viaje.getVehiculo().getId());
        dto.setMatricula(viaje.getVehiculo().getMatricula());
        dto.setIdChofer(viaje.getChofer().getCiudadano().getId());
        dto.setCedulaChofer(viaje.getChofer().getCiudadano().getCedula());
        return dto;
    }

    public static Viaje toEntity(Guia guia, Vehiculo vehiculo, Chofer chofer) {
        return new Viaje(guia, vehiculo, chofer);
    }

}
