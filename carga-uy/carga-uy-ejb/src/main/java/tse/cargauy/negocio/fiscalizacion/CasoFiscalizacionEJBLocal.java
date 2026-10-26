package tse.cargauy.negocio.fiscalizacion;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.DescargoDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.FiltroCasos;
import tse.cargauy.dtos.NotificacionDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PistaAuditoriaDto;
import tse.cargauy.dtos.ResolucionDto;
import tse.cargauy.entities.EstadoCaso;

@Local
public interface CasoFiscalizacionEJBLocal {

    CasoFiscalizacionDto registrarCaso(CasoFiscalizacionDto casoDto, List<EvidenciaDto> evidencias);

    PaginaDto<CasoFiscalizacionDto> listar(FiltroCasos filtro, Paginacion paginacion);

    CasoFiscalizacionDto getCaso(Long id);

    List<PistaAuditoriaDto> getAuditoria(Long id);

    CasoFiscalizacionDto resolver(Long id, ResolucionDto resolucion);

    PaginaDto<CasoFiscalizacionDto> listarInfracciones(Long idEmpresa, EstadoCaso estado, Paginacion paginacion);

    CasoFiscalizacionDto getInfraccion(Long id);

    CasoFiscalizacionDto presentarDescargo(Long id, DescargoDto descargo);

    PaginaDto<NotificacionDto> getNotificaciones(Paginacion paginacion);

    void marcarNotificacionesLeidas();

    PaginaDto<NotificacionDto> getNotificacionesFuncionario(Paginacion paginacion);

    void marcarNotificacionesFuncionarioLeidas();
}
