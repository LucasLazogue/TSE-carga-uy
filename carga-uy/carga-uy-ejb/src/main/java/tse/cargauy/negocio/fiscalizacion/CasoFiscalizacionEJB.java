package tse.cargauy.negocio.fiscalizacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.fiscalizacion.CasoFiscalizacionDAOLocal;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.DescargoDto;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.FiltroCasos;
import tse.cargauy.dtos.NotificacionDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PistaAuditoriaDto;
import tse.cargauy.dtos.ResolucionDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.NormaFiscalizacion;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;

@Stateless
@RolesAllowed(Rol.FUNCIONARIO)
public class CasoFiscalizacionEJB implements CasoFiscalizacionEJBLocal {

    @EJB
    CasoFiscalizacionDAOLocal casoDAO;

    @EJB
    UsuarioDAOLocal usuarioDAO;

    @EJB
    EmpresaDAOLocal empresaDAO;

    @EJB
    VehiculoDAOLocal vehiculoDAO;

    @EJB
    EmpresaEJBLocal empresaEJB;

    @Resource
    SessionContext contexto;

    // si el hecho ya genero un caso se devuelve ese, asi reprocesar un evento no duplica casos
    // TODO: pasar a notificada recien cuando se confirme el envio del correo; por ahora se notifica en la bandeja web
    public CasoFiscalizacionDto registrarCaso(CasoFiscalizacionDto casoDto, List<EvidenciaDto> evidencias) {
        CasoFiscalizacionDto existente = casoDAO.getByClave(casoDto.getClaveDeduplicacion());
        if (existente != null) {
            return existente;
        }
        if (vehiculoDAO.getVehiculoById(casoDto.getIdVehiculo()) == null) {
            throw new CargaUYException(CodigoError.VEHICULO_NO_ENCONTRADO, String.valueOf(casoDto.getIdVehiculo()));
        }

        LocalDateTime ahora = ahora();
        CasoFiscalizacionDto caso = casoDAO.addCaso(casoDto, evidencias, ahora);
        MaquinaEstadosCaso.validarNotificacion(caso);
        casoDAO.notificar(caso.getId(), ahora, ahora.plusDays(plazoDescargoDias(ahora.toLocalDate())));
        return casoDAO.getCasoById(caso.getId());
    }

    public PaginaDto<CasoFiscalizacionDto> listar(FiltroCasos filtro, Paginacion paginacion) {
        return casoDAO.buscar(filtro, paginacion);
    }

    public CasoFiscalizacionDto getCaso(Long id) {
        return buscarCaso(id);
    }

    public List<PistaAuditoriaDto> getAuditoria(Long id) {
        buscarCaso(id);
        return casoDAO.getAuditoria(id);
    }

    public CasoFiscalizacionDto resolver(Long id, ResolucionDto resolucion) {
        if (resolucion == null) {
            throw new CargaUYException(CodigoError.PARAMETRO_INVALIDO, "resolucion", "null");
        }
        CasoFiscalizacionDto caso = buscarCaso(id);
        LocalDateTime ahora = ahora();
        MaquinaEstadosCaso.validarResolucion(caso, resolucion.getResultado(), resolucion.getFundamento(), ahora);

        casoDAO.resolver(id, idUsuario(), resolucion.getResultado(), resolucion.getFundamento().strip(), ahora);
        return casoDAO.getCasoById(id);
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public PaginaDto<CasoFiscalizacionDto> listarInfracciones(Long idEmpresa, EstadoCaso estado, Paginacion paginacion) {
        FiltroCasos filtro = new FiltroCasos(estado, null, null, null);
        filtro.setSoloNotificados(true);
        if (idEmpresa != null) {
            empresaEJB.validarAcceso(idEmpresa);
            filtro.setIdsEmpresa(List.of(idEmpresa));
        } else {
            filtro.setIdsEmpresa(empresasDelResponsable());
        }
        PaginaDto<CasoFiscalizacionDto> pagina = casoDAO.buscar(filtro, paginacion);
        pagina.getItems().forEach(CasoFiscalizacionEJB::paraLaEmpresa);
        return pagina;
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public CasoFiscalizacionDto getInfraccion(Long id) {
        return paraLaEmpresa(buscarInfraccion(id));
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public CasoFiscalizacionDto presentarDescargo(Long id, DescargoDto descargo) {
        if (descargo == null) {
            throw new CargaUYException(CodigoError.PARAMETRO_INVALIDO, "descargo", "null");
        }
        CasoFiscalizacionDto caso = buscarInfraccion(id);
        LocalDateTime ahora = ahora();
        MaquinaEstadosCaso.validarDescargo(caso, descargo.getTexto(), ahora);

        casoDAO.presentarDescargo(id, idUsuario(), descargo.getTexto().strip(), ahora);
        return paraLaEmpresa(casoDAO.getCasoById(id));
    }

    @RolesAllowed(Rol.RESPONSABLE)
    public PaginaDto<NotificacionDto> getNotificaciones(Paginacion paginacion) {
        return casoDAO.getNotificaciones(empresasDelResponsable(), paginacion);
    }

    private CasoFiscalizacionDto buscarCaso(Long id) {
        CasoFiscalizacionDto caso = casoDAO.getCasoById(id);
        if (caso == null) {
            throw new CargaUYException(CodigoError.CASO_NO_ENCONTRADO, String.valueOf(id));
        }
        return caso;
    }

    // la empresa ve el caso recien cuando se le notifica; si no es suyo responde igual que si no existiera
    private CasoFiscalizacionDto buscarInfraccion(Long id) {
        CasoFiscalizacionDto caso = buscarCaso(id);
        if (caso.getEstado() == EstadoCaso.DETECTADA
                || usuarioDAO.getResponsableVigente(idUsuario(), caso.getIdEmpresa(), ahora().toLocalDate()) == null) {
            throw new CargaUYException(CodigoError.CASO_NO_ENCONTRADO, String.valueOf(id));
        }
        return caso;
    }

    // quien resolvio y la clave interna del hecho quedan para el funcionario
    private static CasoFiscalizacionDto paraLaEmpresa(CasoFiscalizacionDto caso) {
        caso.setClaveDeduplicacion(null);
        if (caso.getResolucion() != null) {
            caso.getResolucion().setCedulaFuncionario(null);
        }
        return caso;
    }

    private List<Long> empresasDelResponsable() {
        return empresaDAO.getByResponsable(idUsuario(), ahora().toLocalDate()).stream().map(EmpresaDto::getId).toList();
    }

    private int plazoDescargoDias(LocalDate fecha) {
        Integer plazo = casoDAO.getNormaVigente(NormaFiscalizacion.PLAZO_DESCARGO_DIAS, fecha);
        if (plazo == null) {
            throw new CargaUYException(CodigoError.NORMA_NO_VIGENTE, NormaFiscalizacion.PLAZO_DESCARGO_DIAS);
        }
        return plazo;
    }

    // package-private para sobrescribirlos en los tests
    Long idUsuario() {
        return Long.valueOf(contexto.getCallerPrincipal().getName());
    }

    LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
