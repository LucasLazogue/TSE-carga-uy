package tse.cargauy.data.posicion;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.entities.EventoPosicion;
import tse.cargauy.entities.NodoPeriferico;
import tse.cargauy.entities.Vehiculo;

@Stateless
public class EventoPosicionDAO implements EventoPosicionDAOLocal {

    // clase 23 del estandar SQL: violacion de una restriccion de integridad (23505 es la de unicidad en PostgreSQL)
    private static final String VIOLACION_INTEGRIDAD = "23";

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    // transaccion propia: si el evento es repetido se deshace solo este insert y no la transaccion del consumidor,
    // que si no volveria a recibir el mismo mensaje una y otra vez
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean registrar(EventoPosicionDto eventoDto) {
        NodoPeriferico nodo = entityManager.find(NodoPeriferico.class, eventoDto.getIdNodo());
        Vehiculo vehiculo = eventoDto.getIdVehiculo() == null ? null : entityManager.find(Vehiculo.class, eventoDto.getIdVehiculo());
        try {
            entityManager.persist(Serializers.toEntity(eventoDto, nodo, vehiculo));
            // sin flush el insert recien se manda al confirmar, fuera de este metodo, y el duplicado no se podria reconocer aca
            entityManager.flush();
            return true;
        } catch (PersistenceException e) {
            if (esViolacionDeIntegridad(e)) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public List<EventoPosicionDto> getRuta(String matricula) {
        List<EventoPosicionDto> res = new ArrayList<>();
        for (EventoPosicion e : entityManager.createNamedQuery(EventoPosicion.RUTA_POR_MATRICULA, EventoPosicion.class)
                .setParameter("matricula", matricula)
                .getResultList()) {
            res.add(Serializers.toDto(e));
        }
        return res;
    }

    private static boolean esViolacionDeIntegridad(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith(VIOLACION_INTEGRIDAD)) {
                return true;
            }
        }
        return false;
    }
}
