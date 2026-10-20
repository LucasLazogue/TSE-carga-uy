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

    public String ultimoMetodo;
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
        llamar("registrarCaso", null);
        return casoDto;
    }

    @Override
    public PaginaDto<CasoFiscalizacionDto> listar(FiltroCasos filtro, Paginacion paginacion) {
        llamar("listar", null);
        ultimoFiltro = filtro;
        ultimaPaginacion = paginacion;
        return pagina(List.of(caso(1L, EstadoCaso.NOTIFICADA)), paginacion);
    }

    @Override
    public CasoFiscalizacionDto getCaso(Long id) {
        llamar("getCaso", id);
        return caso(id, EstadoCaso.NOTIFICADA);
    }

    @Override
    public List<PistaAuditoriaDto> getAuditoria(Long id) {
        llamar("getAuditoria", id);
        PistaAuditoriaDto pista = new PistaAuditoriaDto();
        pista.setIdCaso(id);
        pista.setEstadoNuevo(EstadoCaso.DETECTADA);
        return List.of(pista);
    }

    @Override
    public CasoFiscalizacionDto resolver(Long id, ResolucionDto resolucion) {
        llamar("resolver", id);
        ultimaResolucion = resolucion;
        return caso(id, resolucion.getResultado());
    }

    @Override
    public PaginaDto<CasoFiscalizacionDto> listarInfracciones(Long idEmpresa, EstadoCaso estado, Paginacion paginacion) {
        llamar("listarInfracciones", null);
        ultimaEmpresa = idEmpresa;
        ultimoEstado = estado;
        ultimaPaginacion = paginacion;
        return pagina(List.of(caso(1L, EstadoCaso.NOTIFICADA)), paginacion);
    }

    @Override
    public CasoFiscalizacionDto getInfraccion(Long id) {
        llamar("getInfraccion", id);
        return caso(id, EstadoCaso.NOTIFICADA);
    }

    @Override
    public CasoFiscalizacionDto presentarDescargo(Long id, DescargoDto descargo) {
        llamar("presentarDescargo", id);
        ultimoDescargo = descargo;
        return caso(id, EstadoCaso.EN_DESCARGO);
    }

    @Override
    public PaginaDto<NotificacionDto> getNotificaciones(Paginacion paginacion) {
        llamar("getNotificaciones", null);
        ultimaPaginacion = paginacion;
        NotificacionDto notificacion = new NotificacionDto();
        notificacion.setIdCaso(1L);
        return pagina(List.of(notificacion), paginacion);
    }

    private void llamar(String metodo, Long id) {
        ultimoMetodo = metodo;
        ultimoId = id;
        if (error != null) {
            throw error;
        }
    }

    private static CasoFiscalizacionDto caso(Long id, EstadoCaso estado) {
        CasoFiscalizacionDto caso = new CasoFiscalizacionDto();
        caso.setId(id);
        caso.setEstado(estado);
        return caso;
    }

    private static <T> PaginaDto<T> pagina(List<T> items, Paginacion paginacion) {
        return new PaginaDto<>(items, items.size(), paginacion.getPagina(), paginacion.getTamanio());
    }
}
