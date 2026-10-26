package tse.cargauy.data.itv;

import tse.cargauy.dtos.ItvDto;
import tse.cargauy.entities.Itv;
import tse.cargauy.entities.Vehiculo;

public class Serializers {
    public static ItvDto toDto(Itv itv) {
        ItvDto dto = new ItvDto(itv.getVehiculo().getId(), itv.getFechaInspeccion(), itv.getFechaVencimiento());
        dto.setId(itv.getId());
        return dto;
    }

    public static Itv toEntity(ItvDto dto, Vehiculo vehiculo) {
        return new Itv(vehiculo, dto.getFechaInspeccion(), dto.getFechaVencimiento());
    }

}
