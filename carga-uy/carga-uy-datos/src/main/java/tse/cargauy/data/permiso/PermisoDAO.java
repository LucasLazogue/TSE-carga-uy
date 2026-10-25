package tse.cargauy.data.permiso;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import tse.cargauy.dtos.FiltroPermisos;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.entities.EstadoPermiso;
import tse.cargauy.entities.Funcionario;
import tse.cargauy.entities.Permiso;
import tse.cargauy.entities.Permiso_;
import tse.cargauy.entities.Vehiculo;
import tse.cargauy.entities.Vehiculo_;

@Stateless
public class PermisoDAO implements PermisoDAOLocal {

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public PermisoDto getPermisoById(Long id) {
        Permiso permiso = entityManager.find(Permiso.class, id);
        if (permiso != null) {
            return Serializers.toDto(permiso);
        }
        return null;
    }

    @Override
    public PermisoDto getPermisoByNro(String nroPermiso) {
        List<Permiso> permisos = entityManager.createQuery(
                        "SELECT p FROM Permiso p WHERE p.nroPermiso = :nro", Permiso.class)
                .setParameter("nro", nroPermiso)
                .getResultList();
        if (permisos.isEmpty()) {
            return null;
        }
        return Serializers.toDto(permisos.get(0));
    }

    @Override
    public PaginaDto<PermisoDto> getAll(Paginacion paginacion) {
        List<PermisoDto> res = new ArrayList<>();

        List<Permiso> permisos = entityManager.createQuery("SELECT p FROM Permiso p ORDER BY p.validoDesde DESC, p.id DESC", Permiso.class)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultList();
        for (Permiso p : permisos) {
            res.add(Serializers.toDto(p));
        }
        long total = entityManager.createQuery("SELECT COUNT(p) FROM Permiso p", Long.class).getSingleResult();
        return new PaginaDto<>(res, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public PaginaDto<PermisoDto> getAll(FiltroPermisos filtro, Paginacion paginacion) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Permiso> query = cb.createQuery(Permiso.class);
        Root<Permiso> permiso = query.from(Permiso.class);
        query.select(permiso)
                .where(condiciones(filtro, cb, permiso))
                .orderBy(cb.desc(permiso.get(Permiso_.validoDesde)), cb.desc(permiso.get(Permiso_.id)));
        List<PermisoDto> items = entityManager.createQuery(query)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultList()
                .stream()
                .map(Serializers::toDto)
                .toList();

        CriteriaQuery<Long> conteo = cb.createQuery(Long.class);
        Root<Permiso> contado = conteo.from(Permiso.class);
        conteo.select(cb.count(contado)).where(condiciones(filtro, cb, contado));
        long total = entityManager.createQuery(conteo).getSingleResult();

        return new PaginaDto<>(items, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    private Predicate[] condiciones(FiltroPermisos filtro, CriteriaBuilder cb, Root<Permiso> permiso) {
        List<Predicate> condiciones = new ArrayList<>();
        if (filtro.getIdVehiculo() != null) {
            condiciones.add(cb.equal(permiso.get(Permiso_.vehiculo).get(Vehiculo_.id), filtro.getIdVehiculo()));
        }
        if (filtro.getEstado() != null) {
            condiciones.add(cb.equal(permiso.get(Permiso_.estado), filtro.getEstado()));
        }
        return condiciones.toArray(Predicate[]::new);
    }

    @Override
    public PaginaDto<PermisoDto> getByVehiculo(Long idVehiculo, Paginacion paginacion) {
        List<PermisoDto> res = new ArrayList<>();

        List<Permiso> permisos = entityManager.createQuery(
                        "SELECT p FROM Permiso p WHERE p.vehiculo.id = :idVehiculo ORDER BY p.validoDesde DESC, p.id DESC", Permiso.class)
                .setParameter("idVehiculo", idVehiculo)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultList();
        for (Permiso p : permisos) {
            res.add(Serializers.toDto(p));
        }
        long total = entityManager.createQuery(
                        "SELECT COUNT(p) FROM Permiso p WHERE p.vehiculo.id = :idVehiculo", Long.class)
                .setParameter("idVehiculo", idVehiculo)
                .getSingleResult();
        return new PaginaDto<>(res, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public PermisoDto getSuperpuesto(Long idVehiculo, LocalDate desde, LocalDate hasta, Long idExcluir) {
        String excluir = idExcluir == null ? "" : " AND p.id <> :idExcluir";
        TypedQuery<Permiso> query = entityManager.createQuery(
                        "SELECT p FROM Permiso p WHERE p.vehiculo.id = :idVehiculo"
                                + " AND p.validoDesde <= :hasta AND p.validoHasta >= :desde" + excluir
                                + " ORDER BY p.validoDesde", Permiso.class)
                .setParameter("idVehiculo", idVehiculo)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta);
        if (idExcluir != null) {
            query.setParameter("idExcluir", idExcluir);
        }
        List<Permiso> permisos = query.setMaxResults(1).getResultList();
        return permisos.isEmpty() ? null : Serializers.toDto(permisos.get(0));
    }

    @Override
    public PermisoDto getVigente(Long idVehiculo, LocalDate fecha) {
        List<Permiso> permisos = entityManager.createQuery(
                        "SELECT p FROM Permiso p WHERE p.vehiculo.id = :idVehiculo AND p.estado = :aprobado"
                                + " AND p.validoDesde <= :fecha AND p.validoHasta >= :fecha", Permiso.class)
                .setParameter("idVehiculo", idVehiculo)
                .setParameter("aprobado", EstadoPermiso.APROBADO)
                .setParameter("fecha", fecha)
                .getResultList();
        if (permisos.isEmpty()) {
            return null;
        }
        return Serializers.toDto(permisos.get(0));
    }

    @Override
    public void addPermiso(PermisoDto permisoDto) {
        Vehiculo vehiculo = entityManager.find(Vehiculo.class, permisoDto.getIdVehiculo());
        Permiso permiso = Serializers.toEntity(permisoDto, vehiculo);
        entityManager.persist(permiso);
    }

    @Override
    public void updatePermiso(Long id, PermisoDto permisoDto) {
        Permiso permiso = entityManager.find(Permiso.class, id);
        if (permiso != null) {
            permiso.setNroPermiso(permisoDto.getNroPermiso());
            permiso.setValidoDesde(permisoDto.getValidoDesde());
            permiso.setValidoHasta(permisoDto.getValidoHasta());
            permiso.setVehiculo(entityManager.find(Vehiculo.class, permisoDto.getIdVehiculo()));
            permiso.setEstado(permisoDto.getEstado());
            permiso.setMotivoRechazo(null);
            permiso.setRevisor(null);
            permiso.setFechaRevision(null);
        }
    }

    @Override
    public void aprobar(Long id, LocalDate desde, LocalDate hasta, Long idFuncionario, LocalDateTime fecha) {
        Permiso permiso = revisar(id, EstadoPermiso.APROBADO, idFuncionario, fecha);
        permiso.setValidoDesde(desde);
        permiso.setValidoHasta(hasta);
    }

    @Override
    public void rechazar(Long id, String motivo, Long idFuncionario, LocalDateTime fecha) {
        revisar(id, EstadoPermiso.RECHAZADO, idFuncionario, fecha).setMotivoRechazo(motivo);
    }

    private Permiso revisar(Long id, EstadoPermiso estado, Long idFuncionario, LocalDateTime fecha) {
        Permiso permiso = entityManager.find(Permiso.class, id);
        permiso.setEstado(estado);
        permiso.setRevisor(entityManager.find(Funcionario.class, idFuncionario));
        permiso.setFechaRevision(fecha);
        return permiso;
    }

    @Override
    public void deletePermiso(Long id) {
        Permiso permiso = entityManager.find(Permiso.class, id);
        if (permiso != null) {
            entityManager.remove(permiso);
        }
    }
}
