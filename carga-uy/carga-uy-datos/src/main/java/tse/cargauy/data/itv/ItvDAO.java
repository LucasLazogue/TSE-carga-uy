package tse.cargauy.data.itv;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.entities.Itv;
import tse.cargauy.entities.Vehiculo;

@Stateless
public class ItvDAO implements ItvDAOLocal {

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public ItvDto getItvById(Long id) {
        Itv itv = entityManager.find(Itv.class, id);
        if (itv != null) {
            return Serializers.toDto(itv);
        }
        return null;
    }

    @Override
    public PaginaDto<ItvDto> getAll(Paginacion paginacion) {
        List<ItvDto> res = new ArrayList<>();

        List<Itv> itvs = entityManager.createQuery("SELECT i FROM Itv i ORDER BY i.fechaVencimiento DESC, i.id DESC", Itv.class)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultList();
        for (Itv i : itvs) {
            res.add(Serializers.toDto(i));
        }
        long total = entityManager.createQuery("SELECT COUNT(i) FROM Itv i", Long.class).getSingleResult();
        return new PaginaDto<>(res, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public List<ItvDto> getByVehiculo(Long idVehiculo) {
        List<ItvDto> res = new ArrayList<>();

        List<Itv> itvs = entityManager.createQuery(
                        "SELECT i FROM Itv i WHERE i.vehiculo.id = :idVehiculo ORDER BY i.fechaVencimiento DESC, i.id DESC", Itv.class)
                .setParameter("idVehiculo", idVehiculo)
                .getResultList();
        for (Itv i : itvs) {
            res.add(Serializers.toDto(i));
        }
        return res;
    }

    @Override
    public ItvDto getVigente(Long idVehiculo, LocalDate fecha) {
        List<Itv> itvs = entityManager.createQuery(
                        "SELECT i FROM Itv i WHERE i.vehiculo.id = :idVehiculo"
                                + " AND i.fechaInspeccion <= :fecha AND i.fechaVencimiento >= :fecha", Itv.class)
                .setParameter("idVehiculo", idVehiculo)
                .setParameter("fecha", fecha)
                .getResultList();
        if (itvs.isEmpty()) {
            return null;
        }
        return Serializers.toDto(itvs.get(0));
    }

    @Override
    public void addItv(ItvDto itvDto) {
        Vehiculo vehiculo = entityManager.find(Vehiculo.class, itvDto.getIdVehiculo());
        entityManager.persist(Serializers.toEntity(itvDto, vehiculo));
    }

    @Override
    public void deleteItv(Long id) {
        Itv itv = entityManager.find(Itv.class, id);
        if (itv != null) {
            entityManager.remove(itv);
        }
    }
}
