package tse.cargauy.data.eventoviaje;

import java.time.Instant;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.entities.EventoViaje;
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

    @Override
    public PaginaDto<EventoViajeDto> getByViaje(Long idViaje, Paginacion paginacion) {
        List<EventoViajeDto> eventos = entityManager.createQuery(
                        "SELECT e FROM EventoViaje e WHERE e.idViaje = :idViaje ORDER BY e.timestampGeneracion, e.id", EventoViaje.class)
                .setParameter("idViaje", idViaje)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultStream()
                .map(Serializers::toDto)
                .toList();
        long total = entityManager.createQuery(
                        "SELECT COUNT(e) FROM EventoViaje e WHERE e.idViaje = :idViaje", Long.class)
                .setParameter("idViaje", idViaje)
                .getSingleResult();
        return new PaginaDto<>(eventos, total, paginacion.getPagina(), paginacion.getTamanio());
    }
}
