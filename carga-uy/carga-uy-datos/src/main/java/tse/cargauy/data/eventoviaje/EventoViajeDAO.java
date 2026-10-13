package tse.cargauy.data.eventoviaje;

import java.time.Instant;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.entities.ResultadoEvento;

@Stateless
public class EventoViajeDAO implements EventoViajeDAOLocal {
    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    // se busca por idEvento (el UUID del celular), no por la clave interna: find() solo sirve para la clave
    @Override
    public boolean existe(String idEvento) {
        Long cantidad = entityManager.createQuery(
                        "SELECT COUNT(e) FROM EventoViaje e WHERE e.idEvento = :idEvento", Long.class)
                .setParameter("idEvento", idEvento)
                .getSingleResult();
        return cantidad > 0;
    }

    // sin REQUIRES_NEW: va en la transaccion de quien llama, asi el evento y el cambio de estado del viaje se confirman juntos
    @Override
    public void registrar(EventoViajeDto evento, Long idChofer, ResultadoEvento resultado, String detalle) {
        entityManager.persist(Serializers.toEntity(evento, idChofer, Instant.now(), resultado, detalle));
    }
}
