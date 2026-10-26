package tse.cargauy.data.informe;

import tse.cargauy.dtos.InformeDto;
import tse.cargauy.entities.Informe;
import tse.cargauy.entities.TipoCarga;
import tse.cargauy.entities.UsuarioBackoffice;

public class Serializers {
    public static InformeDto toDto(Informe informe) {
        InformeDto dto = new InformeDto();
        dto.setId(informe.getId());
        dto.setTipo(informe.getTipo());
        dto.setPeriodoDesde(informe.getPeriodoDesde());
        dto.setPeriodoHasta(informe.getPeriodoHasta());
        dto.setIdTipoCarga(informe.getTipoCarga() == null ? null : informe.getTipoCarga().getId());
        dto.setTipoCarga(informe.getTipoCarga() == null ? null : informe.getTipoCarga().getNombre());
        dto.setFormato(informe.getFormato());
        dto.setEstado(informe.getEstado());
        dto.setFechaSolicitud(informe.getFechaSolicitud());
        dto.setFechaGeneracion(informe.getFechaGeneracion());
        dto.setIdSolicitante(informe.getSolicitante().getId());
        return dto;
    }

    public static Informe toEntity(InformeDto dto, TipoCarga tipoCarga, UsuarioBackoffice solicitante) {
        Informe informe = new Informe();
        informe.setTipo(dto.getTipo());
        informe.setPeriodoDesde(dto.getPeriodoDesde());
        informe.setPeriodoHasta(dto.getPeriodoHasta());
        informe.setTipoCarga(tipoCarga);
        informe.setFormato(dto.getFormato());
        informe.setEstado(dto.getEstado());
        informe.setFechaSolicitud(dto.getFechaSolicitud());
        informe.setSolicitante(solicitante);
        return informe;
    }
}
