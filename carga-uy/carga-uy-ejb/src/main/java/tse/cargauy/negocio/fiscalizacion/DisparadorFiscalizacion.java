package tse.cargauy.negocio.fiscalizacion;

import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import tse.cargauy.negocio.tracking.PosicionProcesada;
import tse.cargauy.negocio.viaje.ViajeIniciadoOFinalizado;

// evalua recien cuando se confirmo la transaccion: una posicion o un fin de viaje que se deshizo no abre casos
@ApplicationScoped
public class DisparadorFiscalizacion {

    private static final Logger LOGGER = Logger.getLogger(DisparadorFiscalizacion.class.getName());

    @EJB
    MotorFiscalizacionEJBLocal motor;

    void alProcesarPosicion(@Observes(during = TransactionPhase.AFTER_SUCCESS) PosicionProcesada posicion) {
        try {
            motor.evaluarPosicion(posicion.idNodo(), posicion.idEvento());
        } catch (RuntimeException e) {
            LOGGER.warning("fiscalizacion evaluacion=FALLIDA evento=" + posicion.idEvento() + " error=" + e);
        }
    }

    void alConfirmarViaje(@Observes(during = TransactionPhase.AFTER_SUCCESS) ViajeIniciadoOFinalizado cambio) {
        try {
            motor.evaluarViaje(cambio.idViaje());
        } catch (RuntimeException e) {
            LOGGER.warning("fiscalizacion evaluacion=FALLIDA viaje=" + cambio.idViaje() + " error=" + e);
        }
    }
}
