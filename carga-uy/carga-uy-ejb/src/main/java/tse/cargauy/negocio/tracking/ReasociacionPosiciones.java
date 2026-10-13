package tse.cargauy.negocio.tracking;

import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import tse.cargauy.negocio.viaje.ViajeIniciadoOFinalizado;

// reasocia las posiciones recien cuando se confirmo el inicio o el fin del viaje: si la transaccion se deshace,
// las posiciones no quedan asociadas a un horario que no se guardo
@ApplicationScoped
public class ReasociacionPosiciones {

    private static final Logger LOGGER = Logger.getLogger(ReasociacionPosiciones.class.getName());

    @EJB
    ReasociadorPosiciones reasociador;

    void alConfirmar(@Observes(during = TransactionPhase.AFTER_SUCCESS) ViajeIniciadoOFinalizado cambio) {
        try {
            reasociador.reasociar(cambio.idViaje());
        } catch (RuntimeException e) {
            // el cambio del viaje ya se confirmo: las posiciones quedan como estaban hasta el proximo inicio o fin
            LOGGER.warning("ingesta reasociacion=FALLIDA viaje=" + cambio.idViaje() + " error=" + e);
        }
    }
}
