package tse.cargauy.data.pesaje;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import tse.cargauy.dtos.PesajeDto;
import tse.cargauy.entities.Pesaje;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
public class PesajeDAO implements PesajeDAOLocal {

    // unique_violation de PostgreSQL
    private static final String CLAVE_DUPLICADA = "23505";

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

    // dos instancias del central pueden guardar el mismo pesaje a la vez: la restriccion unica de idOrigen
    // rechaza al segundo, que se entera aca por el flush
    @Override
    public void registrar(PesajeDto pesaje) {
        entityManager.persist(Serializers.toEntity(pesaje));
        try {
            entityManager.flush();
        } catch (PersistenceException e) {
            if (esClaveDuplicada(e)) {
                throw new CargaUYException(CodigoError.PESAJE_YA_REGISTRADO, pesaje.getIdOrigen());
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
