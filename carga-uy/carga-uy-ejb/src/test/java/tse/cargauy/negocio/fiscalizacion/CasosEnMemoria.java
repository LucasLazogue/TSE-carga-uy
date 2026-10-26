package tse.cargauy.negocio.fiscalizacion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tse.cargauy.data.fiscalizacion.CasoFiscalizacionDAO;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.DescargoDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.FiltroCasos;
import tse.cargauy.dtos.NotificacionDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PistaAuditoriaDto;
import tse.cargauy.dtos.ResolucionDto;
import tse.cargauy.entities.AccionAuditoria;
import tse.cargauy.entities.DestinatarioNotificacion;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.NormaFiscalizacion;

class CasosEnMemoria extends CasoFiscalizacionDAO {

    final Map<Long, CasoFiscalizacionDto> casos = new LinkedHashMap<>();
    final List<PistaAuditoriaDto> auditoria = new ArrayList<>();
    final List<NotificacionDto> notificaciones = new ArrayList<>();
    final List<NotificacionDto> notificacionesFuncionario = new ArrayList<>();
    Integer plazoDescargo = 10;
    Integer toleranciaExceso = 0;
    Long empresaDeLosVehiculos;
    FiltroCasos ultimoFiltro;

    @Override
    public CasoFiscalizacionDto getCasoById(Long id) {
        return copia(casos.get(id));
    }

    @Override
    public CasoFiscalizacionDto getByClave(String claveDeduplicacion) {
        return casos.values().stream().filter(c -> c.getClaveDeduplicacion().equals(claveDeduplicacion)).findFirst().map(CasosEnMemoria::copia)
                .orElse(null);
    }

    @Override
    public PaginaDto<CasoFiscalizacionDto> buscar(FiltroCasos filtro, Paginacion paginacion) {
        ultimoFiltro = filtro;
        List<CasoFiscalizacionDto> items = casos.values().stream()
                .filter(c -> filtro.getIdsEmpresa() == null || filtro.getIdsEmpresa().contains(c.getIdEmpresa()))
                .filter(c -> !filtro.isSoloNotificados() || c.getEstado() != EstadoCaso.DETECTADA)
                .filter(c -> filtro.getEstado() == null || filtro.getEstado() == c.getEstado())
                .filter(c -> filtro.getTipo() == null || filtro.getTipo() == c.getTipo())
                .filter(c -> filtro.getDesde() == null || !c.getFechaDeteccion().toLocalDate().isBefore(filtro.getDesde()))
                .filter(c -> filtro.getHasta() == null || !c.getFechaDeteccion().toLocalDate().isAfter(filtro.getHasta()))
                .map(CasosEnMemoria::copia)
                .toList();
        return new PaginaDto<>(items, items.size(), paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public CasoFiscalizacionDto addCaso(CasoFiscalizacionDto casoDto, List<EvidenciaDto> evidencias, LocalDateTime fecha) {
        CasoFiscalizacionDto caso = agregar(casoDto.getClaveDeduplicacion(), empresaDeLosVehiculos, EstadoCaso.DETECTADA, null);
        caso.setTipo(casoDto.getTipo());
        caso.setIdVehiculo(casoDto.getIdVehiculo());
        caso.setIdViaje(casoDto.getIdViaje());
        caso.setFechaDeteccion(fecha);
        caso.setEvidencias(new ArrayList<>(evidencias));
        auditar(caso, "SISTEMA", AccionAuditoria.CREAR_CASO, null);
        return caso;
    }

    @Override
    public void agregarEvidencias(Long idCaso, List<EvidenciaDto> evidencias) {
        List<EvidenciaDto> delCaso = casos.get(idCaso).getEvidencias();
        for (EvidenciaDto e : evidencias) {
            if (delCaso.stream().noneMatch(d -> d.getIdOrigen().equals(e.getIdOrigen()))) {
                delCaso.add(e);
            }
        }
    }

    @Override
    public void notificar(Long id, LocalDateTime fecha, LocalDateTime venceDescargo) {
        CasoFiscalizacionDto caso = casos.get(id);
        EstadoCaso anterior = caso.getEstado();
        caso.setEstado(EstadoCaso.NOTIFICADA);
        caso.setFechaNotificacion(fecha);
        caso.setVenceDescargo(venceDescargo);
        avisar(notificaciones, caso);
        auditar(caso, "SISTEMA", AccionAuditoria.NOTIFICAR, anterior);
    }

    @Override
    public void presentarDescargo(Long id, Long idCiudadano, String texto, LocalDateTime fecha) {
        CasoFiscalizacionDto caso = casos.get(id);
        EstadoCaso anterior = caso.getEstado();
        DescargoDto descargo = new DescargoDto(texto);
        descargo.setCedulaPresentadoPor(String.valueOf(idCiudadano));
        caso.setDescargo(descargo);
        caso.setEstado(EstadoCaso.EN_DESCARGO);
        avisar(notificacionesFuncionario, caso);
        auditar(caso, String.valueOf(idCiudadano), AccionAuditoria.PRESENTAR_DESCARGO, anterior);
    }

    @Override
    public void resolver(Long id, Long idFuncionario, EstadoCaso resultado, String fundamento, LocalDateTime fecha) {
        CasoFiscalizacionDto caso = casos.get(id);
        EstadoCaso anterior = caso.getEstado();
        ResolucionDto resolucion = new ResolucionDto(resultado, fundamento);
        resolucion.setCedulaFuncionario(String.valueOf(idFuncionario));
        caso.setResolucion(resolucion);
        caso.setEstado(resultado);
        avisar(notificaciones, caso);
        auditar(caso, String.valueOf(idFuncionario),
                resultado == EstadoCaso.CONFIRMADA ? AccionAuditoria.CONFIRMAR : AccionAuditoria.DESESTIMAR, anterior);
    }

    @Override
    public List<PistaAuditoriaDto> getAuditoria(Long idCaso) {
        return auditoria.stream().filter(p -> p.getIdCaso().equals(idCaso)).toList();
    }

    @Override
    public PaginaDto<NotificacionDto> getNotificaciones(DestinatarioNotificacion destinatario, List<Long> idsEmpresa, Paginacion paginacion) {
        List<NotificacionDto> items = delDestinatario(destinatario, idsEmpresa).stream().map(CasosEnMemoria::copia).toList();
        return new PaginaDto<>(items, items.size(), paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public void marcarLeidas(DestinatarioNotificacion destinatario, List<Long> idsEmpresa) {
        delDestinatario(destinatario, idsEmpresa).forEach(n -> n.setLeida(true));
    }

    @Override
    public Integer getNormaVigente(String clave, LocalDate fecha) {
        return NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA.equals(clave) ? toleranciaExceso : plazoDescargo;
    }

    CasoFiscalizacionDto agregar(String clave, Long idEmpresa, EstadoCaso estado, LocalDateTime venceDescargo) {
        CasoFiscalizacionDto caso = new CasoFiscalizacionDto();
        caso.setId((long) casos.size() + 1);
        caso.setClaveDeduplicacion(clave);
        caso.setIdEmpresa(idEmpresa);
        caso.setEstado(estado);
        caso.setVenceDescargo(venceDescargo);
        casos.put(caso.getId(), caso);
        return caso;
    }

    // como el dao real, cada lectura devuelve un dto nuevo
    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T copia(T dto) {
        if (dto == null) {
            return null;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
                salida.writeObject(dto);
            }
            try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (T) entrada.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<NotificacionDto> delDestinatario(DestinatarioNotificacion destinatario, List<Long> idsEmpresa) {
        List<NotificacionDto> lista = destinatario == DestinatarioNotificacion.EMPRESA ? notificaciones : notificacionesFuncionario;
        return lista.stream().filter(n -> idsEmpresa == null || idsEmpresa.contains(n.getIdEmpresa())).toList();
    }

    private static void avisar(List<NotificacionDto> lista, CasoFiscalizacionDto caso) {
        NotificacionDto notificacion = new NotificacionDto();
        notificacion.setIdCaso(caso.getId());
        notificacion.setIdEmpresa(caso.getIdEmpresa());
        lista.add(notificacion);
    }

    private void auditar(CasoFiscalizacionDto caso, String usuario, AccionAuditoria accion, EstadoCaso anterior) {
        PistaAuditoriaDto registro = new PistaAuditoriaDto();
        registro.setIdCaso(caso.getId());
        registro.setUsuario(usuario);
        registro.setAccion(accion);
        registro.setEstadoAnterior(anterior);
        registro.setEstadoNuevo(caso.getEstado());
        auditoria.add(registro);
    }
}
