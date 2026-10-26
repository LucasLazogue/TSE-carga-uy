package tse.cargauy.negocio.informe;

import java.util.logging.Logger;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/queue_informes"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class InformesMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(InformesMDB.class.getName());

    @EJB
    GeneradorInformesEJBLocal generador;

    @Override
    public void onMessage(Message mensaje) {
        Long idInforme;
        try {
            idInforme = Long.valueOf(((TextMessage) mensaje).getText());
        } catch (JMSException | ClassCastException | NumberFormatException e) {
            LOGGER.warning("informe mensaje=MAL_FORMADO error=" + e);
            return;
        }
        generador.generar(idInforme);
    }
}
