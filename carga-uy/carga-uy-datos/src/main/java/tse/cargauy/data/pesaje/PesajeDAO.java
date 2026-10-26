package tse.cargauy.data.pesaje;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.PesajeDto;
import tse.cargauy.entities.Pesaje;

@Stateless
public class PesajeDAO implements PesajeDAOLocal {
    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public boolean existe(String idOrigen) {
        Long cantidad = entityManager.createQuery(
                        "SELECT COUNT(p) FROM Pesaje p WHERE p.idOrigen = :idOrigen", Long.class)
                .setParameter("idOrigen", idOrigen)
                .getSingleResult();
        return cantidad > 0;
    }

    @Override
    public void registrar(PesajeDto pesaje) {
        entityManager.persist(Serializers.toEntity(pesaje));
    }

    @Override
    public List<PesajeDto> getByVehiculo(Long idVehiculo, LocalDateTime desde, LocalDateTime hasta) {
        return entityManager.createQuery(
                        "SELECT p FROM Pesaje p WHERE p.idVehiculo = :idVehiculo"
                                + " AND p.fechaHora >= :desde AND p.fechaHora < :hasta ORDER BY p.fechaHora", Pesaje.class)
                .setParameter("idVehiculo", idVehiculo)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultStream()
                .map(Serializers::toDto)
                .toList();
    }
}
