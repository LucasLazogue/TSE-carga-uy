package tse.cargauy.data.guia;

import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.entities.Ciudadano;
import tse.cargauy.entities.Empresa;
import tse.cargauy.entities.Guia;
import tse.cargauy.entities.Rubro;
import tse.cargauy.entities.TipoCarga;

public class Serializers {
    public static GuiaDto toDto(Guia guia) {
        GuiaDto dto = new GuiaDto();
        dto.setId(guia.getId());
        dto.setNroGuia(guia.getNroGuia());
        dto.setFecha(guia.getFecha());
        dto.setOrigenLat(guia.getOrigenLat());
        dto.setOrigenLon(guia.getOrigenLon());
        dto.setDestinoLat(guia.getDestinoLat());
        dto.setDestinoLon(guia.getDestinoLon());
        dto.setVolumen(guia.getVolumen());
        dto.setIdEmpresa(guia.getEmpresa().getId());
        dto.setNombreEmpresa(guia.getEmpresa().getNombrePublico());
        dto.setIdRegistradaPor(guia.getRegistradaPor().getId());
        dto.setIdRubro(guia.getRubro().getId());
        dto.setNombreRubro(guia.getRubro().getNombre());
        dto.setIdTipoCarga(guia.getTipoCarga().getId());
        dto.setNombreTipoCarga(guia.getTipoCarga().getNombre());
        if (guia.getViaje() != null) {
            dto.setIdViaje(guia.getViaje().getId());
        }
        return dto;
    }

    public static Guia toEntity(GuiaDto dto, Empresa empresa, Ciudadano registradaPor, Rubro rubro, TipoCarga tipoCarga) {
        return new Guia(dto.getFecha(), dto.getOrigenLat(), dto.getOrigenLon(), dto.getDestinoLat(), dto.getDestinoLon(), dto.getVolumen(),
                empresa, registradaPor, rubro, tipoCarga);
    }

    public static RubroDto toDto(Rubro rubro) {
        RubroDto dto = new RubroDto(rubro.getNombre());
        dto.setId(rubro.getId());
        return dto;
    }

    public static TipoCargaDto toDto(TipoCarga tipoCarga) {
        TipoCargaDto dto = new TipoCargaDto(tipoCarga.getNombre());
        dto.setId(tipoCarga.getId());
        return dto;
    }

}
