package tse.cargauy.data.empresa;

import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.entities.Empresa;

public class Serializers {
    public static EmpresaDto toDto(Empresa empresa) {
        EmpresaDto dto = new EmpresaDto();
        dto.setId(empresa.getId());
        dto.setNroEmpresa(empresa.getNroEmpresa());
        dto.setNombrePublico(empresa.getNombrePublico());
        dto.setRazonSocial(empresa.getRazonSocial());
        dto.setDireccionPrincipal(empresa.getDireccionPrincipal());
        dto.setFechaAlta(empresa.getFechaAlta());
        return dto;
    }

    // sin id: el id lo genera la base al hacer persist
    public static Empresa toEntity(EmpresaDto dto) {
        Empresa empresa = new Empresa();
        empresa.setNroEmpresa(dto.getNroEmpresa());
        empresa.setNombrePublico(dto.getNombrePublico());
        empresa.setRazonSocial(dto.getRazonSocial());
        empresa.setDireccionPrincipal(dto.getDireccionPrincipal());
        empresa.setFechaAlta(dto.getFechaAlta());
        return empresa;
    }

}
