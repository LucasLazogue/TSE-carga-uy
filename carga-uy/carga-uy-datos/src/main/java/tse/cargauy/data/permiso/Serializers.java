package tse.cargauy.data.permiso;

import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.entities.Permiso;
import tse.cargauy.entities.Vehiculo;

public class Serializers {
    public static PermisoDto toDto(Permiso permiso) {
        PermisoDto dto = new PermisoDto();
        dto.setId(permiso.getId());
        dto.setNroPermiso(permiso.getNroPermiso());
        dto.setValidoDesde(permiso.getValidoDesde());
        dto.setValidoHasta(permiso.getValidoHasta());
        dto.setIdVehiculo(permiso.getVehiculo().getId());
        dto.setMatricula(permiso.getVehiculo().getMatricula());
        return dto;
    }

    public static Permiso toEntity(PermisoDto dto, Vehiculo vehiculo) {
        Permiso permiso = new Permiso();
        permiso.setNroPermiso(dto.getNroPermiso());
        permiso.setValidoDesde(dto.getValidoDesde());
        permiso.setValidoHasta(dto.getValidoHasta());
        permiso.setVehiculo(vehiculo);
        return permiso;
    }

}
