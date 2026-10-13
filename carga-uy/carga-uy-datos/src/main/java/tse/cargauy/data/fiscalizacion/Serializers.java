package tse.cargauy.data.fiscalizacion;

import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.DescargoDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.NotificacionDto;
import tse.cargauy.dtos.PistaAuditoriaDto;
import tse.cargauy.dtos.ResolucionDto;
import tse.cargauy.entities.CasoFiscalizacion;
import tse.cargauy.entities.Descargo;
import tse.cargauy.entities.Evidencia;
import tse.cargauy.entities.Notificacion;
import tse.cargauy.entities.PistaAuditoria;
import tse.cargauy.entities.Resolucion;

public class Serializers {

    public static CasoFiscalizacionDto toDto(CasoFiscalizacion caso, boolean detalle) {
        CasoFiscalizacionDto dto = new CasoFiscalizacionDto();
        dto.setId(caso.getId());
        dto.setTipo(caso.getTipo());
        dto.setEstado(caso.getEstado());
        dto.setClaveDeduplicacion(caso.getClaveDeduplicacion());
        dto.setFechaHecho(caso.getFechaHecho());
        dto.setFechaDeteccion(caso.getFechaDeteccion());
        dto.setFechaNotificacion(caso.getFechaNotificacion());
        dto.setVenceDescargo(caso.getVenceDescargo());
        dto.setIdVehiculo(caso.getVehiculo().getId());
        dto.setMatricula(caso.getVehiculo().getMatricula());
        dto.setIdEmpresa(caso.getEmpresa().getId());
        dto.setNombreEmpresa(caso.getEmpresa().getNombrePublico());
        if (caso.getViaje() != null) {
            dto.setIdViaje(caso.getViaje().getId());
        }
        if (caso.getDescargo() != null) {
            dto.setDescargo(toDto(caso.getDescargo()));
        }
        if (caso.getResolucion() != null) {
            dto.setResolucion(toDto(caso.getResolucion()));
        }
        if (detalle) {
            dto.setEvidencias(caso.getEvidencias().stream().map(Serializers::toDto).toList());
        }
        return dto;
    }

    public static EvidenciaDto toDto(Evidencia evidencia) {
        EvidenciaDto dto = new EvidenciaDto(evidencia.getTipo(), evidencia.getIdOrigen(), evidencia.getFechaEvento(),
                evidencia.getValorObservado(), evidencia.getValorDeclarado(), evidencia.getUmbral(), evidencia.getDescripcion());
        dto.setId(evidencia.getId());
        return dto;
    }

    public static DescargoDto toDto(Descargo descargo) {
        DescargoDto dto = new DescargoDto(descargo.getTexto());
        dto.setId(descargo.getId());
        dto.setFecha(descargo.getFecha());
        dto.setCedulaPresentadoPor(descargo.getPresentadoPor().getCedula());
        return dto;
    }

    public static ResolucionDto toDto(Resolucion resolucion) {
        ResolucionDto dto = new ResolucionDto(resolucion.getResultado(), resolucion.getFundamento());
        dto.setId(resolucion.getId());
        dto.setFecha(resolucion.getFecha());
        dto.setCedulaFuncionario(resolucion.getFuncionario().getCedula());
        return dto;
    }

    public static PistaAuditoriaDto toDto(PistaAuditoria registro) {
        PistaAuditoriaDto dto = new PistaAuditoriaDto();
        dto.setId(registro.getId());
        dto.setIdCaso(registro.getIdCaso());
        dto.setUsuario(registro.getUsuario());
        dto.setRol(registro.getRol());
        dto.setAccion(registro.getAccion());
        dto.setEstadoAnterior(registro.getEstadoAnterior());
        dto.setEstadoNuevo(registro.getEstadoNuevo());
        dto.setFundamento(registro.getFundamento());
        dto.setFecha(registro.getFecha());
        return dto;
    }

    public static NotificacionDto toDto(Notificacion notificacion) {
        NotificacionDto dto = new NotificacionDto();
        dto.setId(notificacion.getId());
        dto.setIdCaso(notificacion.getCaso().getId());
        dto.setIdEmpresa(notificacion.getEmpresa().getId());
        dto.setFecha(notificacion.getFecha());
        dto.setMensaje(notificacion.getMensaje());
        return dto;
    }

}
