package tse.cargauy.negocio.tracking;

import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import tse.cargauy.data.posicion.EventoPosicionDAOLocal;

// marca la posicion como procesada recien cuando se confirmo la transaccion del consumidor. Si se marcara antes y
// la transaccion se deshiciera, el reenvio la encontraria procesada y no repetiria los pasos que se deshicieron
@ApplicationScoped
public class MarcadorPosiciones {

    private static final Logger LOGGER = Logger.getLogger(MarcadorPosiciones.class.getName());

    @EJB
    EventoPosicionDAOLocal eventoDAO;

    void alConfirmar(@Observes(during = TransactionPhase.AFTER_SUCCESS) PosicionProcesada posicion) {
        try {
            eventoDAO.marcarProcesada(posicion.idNodo(), posicion.idEvento());
        } catch (RuntimeException e) {
            // la transaccion ya se confirmo y no se puede deshacer: queda sin marcar, y si llega un reenvio se
            // repiten los pasos siguientes, que se pueden repetir sin efectos
            LOGGER.warning("ingesta marca=NO_PUESTA evento=" + posicion.idEvento() + " error=" + e);
        }
    }
}
