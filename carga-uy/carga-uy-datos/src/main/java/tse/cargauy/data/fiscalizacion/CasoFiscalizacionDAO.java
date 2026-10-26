package tse.cargauy.data.fiscalizacion;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.FiltroCasos;
import tse.cargauy.dtos.NormaFiscalizacionDto;
import tse.cargauy.dtos.NotificacionDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PistaAuditoriaDto;
import tse.cargauy.entities.AccionAuditoria;
import tse.cargauy.entities.CasoFiscalizacion;
import tse.cargauy.entities.CasoFiscalizacion_;
import tse.cargauy.entities.Ciudadano;
import tse.cargauy.entities.Descargo;
import tse.cargauy.entities.DestinatarioNotificacion;
import tse.cargauy.entities.Empresa_;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.Evidencia;
import tse.cargauy.entities.Funcionario;
import tse.cargauy.entities.NormaFiscalizacion;
import tse.cargauy.entities.Notificacion;
import tse.cargauy.entities.PistaAuditoria;
import tse.cargauy.entities.Resolucion;
import tse.cargauy.entities.Rol;
import tse.cargauy.entities.Vehiculo;
import tse.cargauy.entities.Viaje;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
public class CasoFiscalizacionDAO implements CasoFiscalizacionDAOLocal {

    // unique_violation de PostgreSQL
    private static final String CLAVE_DUPLICADA = "23505";

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public CasoFiscalizacionDto getCasoById(Long id) {
        CasoFiscalizacion caso = entityManager.find(CasoFiscalizacion.class, id);
        return caso == null ? null : Serializers.toDto(caso, true);
    }

    @Override
    public CasoFiscalizacionDto getByClave(String claveDeduplicacion) {
        List<CasoFiscalizacion> casos = entityManager.createQuery(
                        "SELECT c FROM CasoFiscalizacion c WHERE c.claveDeduplicacion = :clave", CasoFiscalizacion.class)
                .setParameter("clave", claveDeduplicacion)
                .getResultList();
        return casos.isEmpty() ? null : Serializers.toDto(casos.get(0), false);
    }

    @Override
    public PaginaDto<CasoFiscalizacionDto> buscar(FiltroCasos filtro, Paginacion paginacion) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<CasoFiscalizacion> query = cb.createQuery(CasoFiscalizacion.class);
        Root<CasoFiscalizacion> caso = query.from(CasoFiscalizacion.class);
        query.select(caso)
                .where(condiciones(filtro, cb, caso))
                .orderBy(cb.desc(caso.get(CasoFiscalizacion_.fechaDeteccion)), cb.desc(caso.get(CasoFiscalizacion_.id)));
        List<CasoFiscalizacionDto> items = entityManager.createQuery(query)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultStream()
                .map(c -> Serializers.toDto(c, false))
                .toList();

        CriteriaQuery<Long> conteo = cb.createQuery(Long.class);
        Root<CasoFiscalizacion> contado = conteo.from(CasoFiscalizacion.class);
        conteo.select(cb.count(contado)).where(condiciones(filtro, cb, contado));
        long total = entityManager.createQuery(conteo).getSingleResult();

        return new PaginaDto<>(items, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    private Predicate[] condiciones(FiltroCasos filtro, CriteriaBuilder cb, Root<CasoFiscalizacion> caso) {
        List<Predicate> condiciones = new ArrayList<>();
        if (filtro.getIdsEmpresa() != null) {
            condiciones.add(filtro.getIdsEmpresa().isEmpty()
                    ? cb.disjunction()
                    : caso.get(CasoFiscalizacion_.empresa).get(Empresa_.id).in(filtro.getIdsEmpresa()));
        }
        if (filtro.isSoloNotificados()) {
            condiciones.add(cb.notEqual(caso.get(CasoFiscalizacion_.estado), EstadoCaso.DETECTADA));
        }
        if (filtro.getEstado() != null) {
            condiciones.add(cb.equal(caso.get(CasoFiscalizacion_.estado), filtro.getEstado()));
        }
        if (filtro.getTipo() != null) {
            condiciones.add(cb.equal(caso.get(CasoFiscalizacion_.tipo), filtro.getTipo()));
        }
        if (filtro.getDesde() != null) {
            condiciones.add(cb.greaterThanOrEqualTo(caso.get(CasoFiscalizacion_.fechaDeteccion), filtro.getDesde().atStartOfDay()));
        }
        if (filtro.getHasta() != null) {
            condiciones.add(cb.lessThan(caso.get(CasoFiscalizacion_.fechaDeteccion), filtro.getHasta().plusDays(1).atStartOfDay()));
        }
        return condiciones.toArray(Predicate[]::new);
    }

    @Override
    public CasoFiscalizacionDto addCaso(CasoFiscalizacionDto casoDto, List<EvidenciaDto> evidencias, LocalDateTime fecha) {
        Vehiculo vehiculo = entityManager.find(Vehiculo.class, casoDto.getIdVehiculo());
        Viaje viaje = casoDto.getIdViaje() == null ? null : entityManager.find(Viaje.class, casoDto.getIdViaje());
        CasoFiscalizacion caso = new CasoFiscalizacion(casoDto.getTipo(), casoDto.getClaveDeduplicacion(), casoDto.getFechaHecho(),
                fecha, vehiculo, viaje);
        entityManager.persist(caso);
        for (EvidenciaDto e : evidencias) {
            Evidencia evidencia = new Evidencia(caso, e.getTipo(), e.getIdOrigen(), e.getFechaEvento(), e.getValorObservado(),
                    e.getValorDeclarado(), e.getUmbral(), e.getDescripcion());
            entityManager.persist(evidencia);
            caso.getEvidencias().add(evidencia);
        }
        flush(CodigoError.CASO_YA_REGISTRADO);
        auditar(caso, PistaAuditoria.SISTEMA, PistaAuditoria.SISTEMA, AccionAuditoria.CREAR_CASO, null,
                evidencias.size() + " evidencia(s)", fecha);
        return Serializers.toDto(caso, true);
    }

    @Override
    public void agregarEvidencias(Long idCaso, List<EvidenciaDto> evidencias) {
        CasoFiscalizacion caso = entityManager.find(CasoFiscalizacion.class, idCaso);
        List<String> origenes = caso.getEvidencias().stream().map(Evidencia::getIdOrigen).toList();
        for (EvidenciaDto e : evidencias) {
            if (!origenes.contains(e.getIdOrigen())) {
                entityManager.persist(new Evidencia(caso, e.getTipo(), e.getIdOrigen(), e.getFechaEvento(), e.getValorObservado(),
                        e.getValorDeclarado(), e.getUmbral(), e.getDescripcion()));
            }
        }
    }

    @Override
    public void notificar(Long id, LocalDateTime fecha, LocalDateTime venceDescargo) {
        CasoFiscalizacion caso = entityManager.find(CasoFiscalizacion.class, id);
        EstadoCaso anterior = caso.getEstado();
        String mensaje = "Se detecto un posible incumplimiento (" + caso.getTipo() + ") del vehiculo " + caso.getVehiculo().getMatricula()
                + ". Puede presentar un descargo hasta el " + venceDescargo.format(CasoFiscalizacionDto.FORMATO_FECHA) + ".";
        caso.setEstado(EstadoCaso.NOTIFICADA);
        caso.setFechaNotificacion(fecha);
        caso.setVenceDescargo(venceDescargo);
        entityManager.persist(new Notificacion(caso, DestinatarioNotificacion.EMPRESA, fecha, mensaje));
        auditar(caso, PistaAuditoria.SISTEMA, PistaAuditoria.SISTEMA, AccionAuditoria.NOTIFICAR, anterior, mensaje, fecha);
        flush();
    }

    @Override
    public void presentarDescargo(Long id, Long idCiudadano, String texto, LocalDateTime fecha) {
        CasoFiscalizacion caso = entityManager.find(CasoFiscalizacion.class, id);
        Ciudadano ciudadano = entityManager.find(Ciudadano.class, idCiudadano);
        EstadoCaso anterior = caso.getEstado();
        Descargo descargo = new Descargo(caso, texto, fecha, ciudadano);
        entityManager.persist(descargo);
        caso.setDescargo(descargo);
        caso.setEstado(EstadoCaso.EN_DESCARGO);
        String mensaje = caso.getEmpresa().getNombrePublico() + " presento un descargo en el caso " + caso.getId() + " (" + caso.getTipo()
                + ", vehiculo " + caso.getVehiculo().getMatricula() + ").";
        entityManager.persist(new Notificacion(caso, DestinatarioNotificacion.FUNCIONARIO, fecha, mensaje));
        auditar(caso, ciudadano.getCedula(), Rol.RESPONSABLE, AccionAuditoria.PRESENTAR_DESCARGO, anterior, texto, fecha);
        flush();
    }

    @Override
    public void resolver(Long id, Long idFuncionario, EstadoCaso resultado, String fundamento, LocalDateTime fecha) {
        CasoFiscalizacion caso = entityManager.find(CasoFiscalizacion.class, id);
        Funcionario funcionario = entityManager.find(Funcionario.class, idFuncionario);
        EstadoCaso anterior = caso.getEstado();
        Resolucion resolucion = new Resolucion(caso, resultado, fundamento, fecha, funcionario);
        entityManager.persist(resolucion);
        caso.setResolucion(resolucion);
        caso.setEstado(resultado);
        String mensaje = "El caso " + caso.getId() + " (" + caso.getTipo() + ", vehiculo " + caso.getVehiculo().getMatricula() + ") fue "
                + (resultado == EstadoCaso.CONFIRMADA ? "confirmado" : "desestimado") + ".";
        entityManager.persist(new Notificacion(caso, DestinatarioNotificacion.EMPRESA, fecha, mensaje));
        AccionAuditoria accion = resultado == EstadoCaso.CONFIRMADA ? AccionAuditoria.CONFIRMAR : AccionAuditoria.DESESTIMAR;
        auditar(caso, funcionario.getCedula(), Rol.FUNCIONARIO, accion, anterior, fundamento, fecha);
        flush();
    }

    @Override
    public List<PistaAuditoriaDto> getAuditoria(Long idCaso) {
        return entityManager.createQuery(
                        "SELECT p FROM PistaAuditoria p WHERE p.idCaso = :idCaso ORDER BY p.fecha, p.id", PistaAuditoria.class)
                .setParameter("idCaso", idCaso)
                .getResultStream()
                .map(Serializers::toDto)
                .toList();
    }

    @Override
    public PaginaDto<NotificacionDto> getNotificaciones(DestinatarioNotificacion destinatario, List<Long> idsEmpresa, Paginacion paginacion) {
        if (idsEmpresa != null && idsEmpresa.isEmpty()) {
            return new PaginaDto<>(List.of(), 0, paginacion.getPagina(), paginacion.getTamanio());
        }
        TypedQuery<Notificacion> query = entityManager.createQuery(
                "SELECT n FROM Notificacion n" + filtroNotificaciones(idsEmpresa) + " ORDER BY n.fecha DESC, n.id DESC", Notificacion.class);
        parametrosNotificaciones(query, destinatario, idsEmpresa);
        List<NotificacionDto> items = query
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultStream()
                .map(Serializers::toDto)
                .toList();
        TypedQuery<Long> conteo = entityManager.createQuery("SELECT COUNT(n) FROM Notificacion n" + filtroNotificaciones(idsEmpresa), Long.class);
        parametrosNotificaciones(conteo, destinatario, idsEmpresa);
        long total = conteo.getSingleResult();
        return new PaginaDto<>(items, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public void marcarLeidas(DestinatarioNotificacion destinatario, List<Long> idsEmpresa) {
        if (idsEmpresa != null && idsEmpresa.isEmpty()) {
            return;
        }
        Query update = entityManager.createQuery("UPDATE Notificacion n SET n.leida = true" + filtroNotificaciones(idsEmpresa));
        parametrosNotificaciones(update, destinatario, idsEmpresa);
        update.executeUpdate();
    }

    @Override
    public Integer getNormaVigente(String clave, LocalDate fecha) {
        NormaFiscalizacionDto norma = getNorma(clave, fecha);
        return norma == null ? null : norma.getValor();
    }

    @Override
    public NormaFiscalizacionDto getNorma(String clave, LocalDate fecha) {
        return entityManager.createQuery(
                        "SELECT n FROM NormaFiscalizacion n WHERE n.clave = :clave AND n.vigenteDesde <= :fecha"
                                + " ORDER BY n.vigenteDesde DESC, n.id DESC", NormaFiscalizacion.class)
                .setParameter("clave", clave)
                .setParameter("fecha", fecha)
                .setMaxResults(1)
                .getResultStream()
                .map(Serializers::toDto)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void addNorma(String clave, int valor, LocalDate vigenteDesde) {
        entityManager.persist(new NormaFiscalizacion(clave, valor, vigenteDesde));
    }

    @Override
    public PaginaDto<NormaFiscalizacionDto> getNormas(String clave, Paginacion paginacion) {
        List<NormaFiscalizacionDto> items = entityManager.createQuery(
                        "SELECT n FROM NormaFiscalizacion n WHERE n.clave = :clave ORDER BY n.vigenteDesde DESC, n.id DESC",
                        NormaFiscalizacion.class)
                .setParameter("clave", clave)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultStream()
                .map(Serializers::toDto)
                .toList();
        long total = entityManager.createQuery("SELECT COUNT(n) FROM NormaFiscalizacion n WHERE n.clave = :clave", Long.class)
                .setParameter("clave", clave)
                .getSingleResult();
        return new PaginaDto<>(items, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    private void auditar(CasoFiscalizacion caso, String usuario, String rol, AccionAuditoria accion, EstadoCaso anterior,
            String fundamento, LocalDateTime fecha) {
        entityManager.persist(new PistaAuditoria(caso.getId(), usuario, rol, accion, anterior, caso.getEstado(), fundamento, fecha));
    }

    private static String filtroNotificaciones(List<Long> idsEmpresa) {
        return " WHERE n.destinatario = :destinatario" + (idsEmpresa == null ? "" : " AND n.empresa.id IN :ids");
    }

    private static void parametrosNotificaciones(Query query, DestinatarioNotificacion destinatario, List<Long> idsEmpresa) {
        query.setParameter("destinatario", destinatario);
        if (idsEmpresa != null) {
            query.setParameter("ids", idsEmpresa);
        }
    }

    // dos transacciones sobre la misma version del caso: la segunda en confirmar pierde
    private void flush() {
        flush(CodigoError.CASO_MODIFICADO_CONCURRENTEMENTE);
    }

    private void flush(CodigoError siHayDuplicado) {
        try {
            entityManager.flush();
        } catch (OptimisticLockException e) {
            throw new CargaUYException(CodigoError.CASO_MODIFICADO_CONCURRENTEMENTE);
        } catch (PersistenceException e) {
            if (esClaveDuplicada(e)) {
                throw new CargaUYException(siHayDuplicado);
            }
            throw e;
        }
    }

    private static boolean esClaveDuplicada(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && CLAVE_DUPLICADA.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
