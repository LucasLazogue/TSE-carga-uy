package tse.cargauy.web.ws;

import java.util.List;

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
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.negocio.fiscalizacion.CasoFiscalizacionEJBLocal;

public class CasosEJBFalso implements CasoFiscalizacionEJBLocal {

    public Long ultimoId;
    public FiltroCasos ultimoFiltro;
    public Paginacion ultimaPaginacion;
    public Long ultimaEmpresa;
    public EstadoCaso ultimoEstado;
    public ResolucionDto ultimaResolucion;
    public DescargoDto ultimoDescargo;
    public CargaUYException error;

    @Override
    public CasoFiscalizacionDto registrarCaso(CasoFiscalizacionDto casoDto, List<EvidenciaDto> evidencias) {
        return casoDto;
    }

    @Override
    public PaginaDto<CasoFiscalizacionDto> listar(FiltroCasos filtro, Paginacion paginacion) {
        ultimoFiltro = filtro;
        ultimaPaginacion = paginacion;
        return new PaginaDto<>(List.of(), 0, paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public CasoFiscalizacionDto getCaso(Long id) {
        return caso(id, EstadoCaso.NOTIFICADA);
    }

    @Override
    public List<PistaAuditoriaDto> getAuditoria(Long id) {
        return List.of();
    }

    @Override
    public CasoFiscalizacionDto resolver(Long id, ResolucionDto resolucion) {
        ultimaResolucion = resolucion;
        return caso(id, resolucion.getResultado());
    }

    @Override
    public PaginaDto<CasoFiscalizacionDto> listarInfracciones(Long idEmpresa, EstadoCaso estado, Paginacion paginacion) {
        ultimaEmpresa = idEmpresa;
        ultimoEstado = estado;
        return new PaginaDto<>(List.of(), 0, paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public CasoFiscalizacionDto getInfraccion(Long id) {
        return caso(id, EstadoCaso.NOTIFICADA);
    }

    @Override
    public CasoFiscalizacionDto presentarDescargo(Long id, DescargoDto descargo) {
        ultimoDescargo = descargo;
        return caso(id, EstadoCaso.EN_DESCARGO);
    }

    @Override
    public PaginaDto<NotificacionDto> getNotificaciones(Paginacion paginacion) {
        return new PaginaDto<>(List.of(), 0, paginacion.getPagina(), paginacion.getTamanio());
    }

    private CasoFiscalizacionDto caso(Long id, EstadoCaso estado) {
        if (error != null) {
            throw error;
        }
        ultimoId = id;
        CasoFiscalizacionDto caso = new CasoFiscalizacionDto();
        caso.setId(id);
        caso.setEstado(estado);
        return caso;
    }
}
