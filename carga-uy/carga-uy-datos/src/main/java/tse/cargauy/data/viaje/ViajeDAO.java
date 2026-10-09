package tse.cargauy.data.viaje;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.Chofer;
import tse.cargauy.entities.Empresa_;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.Guia;
import tse.cargauy.entities.Guia_;
import tse.cargauy.entities.Usuario_;
import tse.cargauy.entities.Vehiculo;
import tse.cargauy.entities.Vehiculo_;
import tse.cargauy.entities.Viaje;
import tse.cargauy.entities.Viaje_;
import tse.cargauy.entities.VinculoEmpresa_;

@Stateless
public class ViajeDAO implements ViajeDAOLocal {

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @EJB
    private UsuarioDAOLocal usuarioDAO;

    @Override
    public ViajeDto getViajeById(Long id) {
        Viaje viaje = entityManager.find(Viaje.class, id);
        return viaje == null ? null : Serializers.toDto(viaje);
    }

    @Override
    public ViajeDto getByGuia(Long idGuia) {
        List<Viaje> viajes = entityManager.createQuery(
                        "SELECT v FROM Viaje v WHERE v.guia.id = :idGuia", Viaje.class)
                .setParameter("idGuia", idGuia)
                .getResultList();
        if (viajes.isEmpty()) {
            return null;
        }
        return Serializers.toDto(viajes.get(0));
    }

    @Override
    public PaginaDto<ViajeDto> getAll(FiltroViajes filtro, Paginacion paginacion) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Viaje> query = cb.createQuery(Viaje.class);
        Root<Viaje> viaje = query.from(Viaje.class);
        viaje.fetch(Viaje_.guia);
        viaje.fetch(Viaje_.vehiculo);
        viaje.fetch(Viaje_.chofer).fetch(VinculoEmpresa_.ciudadano);
        query.select(viaje)
                .where(condiciones(filtro, cb, viaje))
                .orderBy(cb.desc(viaje.get(Viaje_.guia).get(Guia_.fecha)), cb.desc(viaje.get(Viaje_.id)));
        List<ViajeDto> items = toDtos(entityManager.createQuery(query)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultList());

        CriteriaQuery<Long> conteo = cb.createQuery(Long.class);
        Root<Viaje> contado = conteo.from(Viaje.class);
        conteo.select(cb.count(contado)).where(condiciones(filtro, cb, contado));
        long total = entityManager.createQuery(conteo).getSingleResult();

        return new PaginaDto<>(items, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    private Predicate[] condiciones(FiltroViajes filtro, CriteriaBuilder cb, Root<Viaje> viaje) {
        List<Predicate> condiciones = new ArrayList<>();
        if (filtro.getIdsEmpresa() != null) {
            condiciones.add(filtro.getIdsEmpresa().isEmpty()
                    ? cb.disjunction()
                    : viaje.get(Viaje_.guia).get(Guia_.empresa).get(Empresa_.id).in(filtro.getIdsEmpresa()));
        }
        if (filtro.getIdChofer() != null) {
            condiciones.add(cb.equal(viaje.get(Viaje_.chofer).get(VinculoEmpresa_.ciudadano).get(Usuario_.id), filtro.getIdChofer()));
        }
        if (filtro.getIdVehiculo() != null) {
            condiciones.add(cb.equal(viaje.get(Viaje_.vehiculo).get(Vehiculo_.id), filtro.getIdVehiculo()));
        }
        if (filtro.getEstado() != null) {
            condiciones.add(cb.equal(viaje.get(Viaje_.estado), filtro.getEstado()));
        }
        return condiciones.toArray(Predicate[]::new);
    }

    @Override
    public ViajeDto addViaje(ViajeDto viajeDto) {
        Guia guia = entityManager.find(Guia.class, viajeDto.getIdGuia());
        Viaje viaje = Serializers.toEntity(guia,
                entityManager.find(Vehiculo.class, viajeDto.getIdVehiculo()),
                getChofer(viajeDto.getIdChofer(), guia));
        entityManager.persist(viaje);
        guia.setViaje(viaje);
        return Serializers.toDto(viaje);
    }

    @Override
    public void updateViaje(Long id, ViajeDto viajeDto) {
        Viaje viaje = entityManager.find(Viaje.class, id);
        if (viaje != null) {
            viaje.setVehiculo(entityManager.find(Vehiculo.class, viajeDto.getIdVehiculo()));
            viaje.setChofer(getChofer(viajeDto.getIdChofer(), viaje.getGuia()));
        }
    }

    @Override
    public void deleteViaje(Long id) {
        Viaje viaje = entityManager.find(Viaje.class, id);
        if (viaje != null) {
            viaje.getGuia().setViaje(null);
            entityManager.remove(viaje);
        }
    }

    @Override
    public ViajeDto getActualDelChofer(Long idCiudadano) {
        Viaje viaje = primeroDelChofer(idCiudadano, EstadoViaje.EN_CURSO);
        if (viaje == null) {
            viaje = primeroDelChofer(idCiudadano, EstadoViaje.ASIGNADO);
        }
        return viaje == null ? null : Serializers.toDto(viaje);
    }

    // la fecha llega del evento del celular, no es la de ahora: el viaje empezo cuando el chofer toco el boton
    @Override
    public void cambiarEstado(Long id, EstadoViaje estado, LocalDateTime fecha) {
        Viaje viaje = entityManager.find(Viaje.class, id);
        if (viaje != null) {
            if (estado == EstadoViaje.EN_CURSO) {
                viaje.setFechaInicio(fecha);
            } else if (estado == EstadoViaje.FINALIZADO) {
                viaje.setFechaFin(fecha);
            }
            viaje.setEstado(estado);
        }
    }

    // fecha de guia ascendente: getAll ordena al reves (lo que quiere el listado) y le daria al chofer el viaje mas lejano
    private Viaje primeroDelChofer(Long idCiudadano, EstadoViaje estado) {
        List<Viaje> viajes = entityManager.createQuery(
                        "SELECT v FROM Viaje v WHERE v.chofer.ciudadano.id = :idCiudadano AND v.estado = :estado"
                                + " ORDER BY v.guia.fecha, v.id", Viaje.class)
                .setParameter("idCiudadano", idCiudadano)
                .setParameter("estado", estado)
                .setMaxResults(1)
                .getResultList();
        return viajes.isEmpty() ? null : viajes.get(0);
    }

    private Chofer getChofer(Long idCiudadano, Guia guia) {
        return entityManager.find(Chofer.class,
                usuarioDAO.getChoferVigente(idCiudadano, guia.getEmpresa().getId(), guia.getFecha()));
    }

    private List<ViajeDto> toDtos(List<Viaje> viajes) {
        List<ViajeDto> res = new ArrayList<>();
        for (Viaje v : viajes) {
            res.add(Serializers.toDto(v));
        }
        return res;
    }
}
