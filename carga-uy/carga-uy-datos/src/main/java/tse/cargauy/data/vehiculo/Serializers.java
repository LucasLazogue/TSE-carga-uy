package tse.cargauy.data.vehiculo;

import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.Empresa;
import tse.cargauy.entities.Vehiculo;

public class Serializers {
    public static VehiculoDto toDto(Vehiculo vehiculo) {
        VehiculoDto dto = new VehiculoDto();
        dto.setId(vehiculo.getId());
        dto.setMatricula(vehiculo.getMatricula());
        dto.setMarca(vehiculo.getMarca());
        dto.setModelo(vehiculo.getModelo());
        dto.setPesoVehiculo(vehiculo.getPesoVehiculo());
        dto.setCapacidadCarga(vehiculo.getCapacidadCarga());
        dto.setIdEmpresa(vehiculo.getEmpresa().getId());
        dto.setNombreEmpresa(vehiculo.getEmpresa().getNombrePublico());
        return dto;
    }

    public static Vehiculo toEntity(VehiculoDto dto, Empresa empresa) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setMatricula(dto.getMatricula());
        vehiculo.setMarca(dto.getMarca());
        vehiculo.setModelo(dto.getModelo());
        vehiculo.setPesoVehiculo(dto.getPesoVehiculo());
        vehiculo.setCapacidadCarga(dto.getCapacidadCarga());
        vehiculo.setEmpresa(empresa);
        return vehiculo;
    }

}
