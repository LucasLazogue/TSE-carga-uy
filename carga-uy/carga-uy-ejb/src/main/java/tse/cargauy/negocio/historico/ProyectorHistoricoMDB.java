package tse.cargauy.negocio.historico;

import java.util.logging.Logger;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.json.bind.JsonbException;
import tse.cargauy.dtos.CasoHistoricoDto;
import tse.cargauy.dtos.ViajeHistoricoDto;
import tse.cargauy.historico.HistoricoDAOLocal;

@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/queue_historico"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class ProyectorHistoricoMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(ProyectorHistoricoMDB.class.getName());

    @EJB
    HistoricoDAOLocal historicoDAO;

    @Override
    public void onMessage(Message mensaje) {
        String tipo;
        String cuerpo;
        try {
            tipo = mensaje.getStringProperty(PublicadorHistorico.TIPO);
            cuerpo = ((TextMessage) mensaje).getText();
        } catch (JMSException | ClassCastException e) {
            LOGGER.warning("historico mensaje=MAL_FORMADO error=" + e);
            return;
        }
        if (tipo == null || cuerpo == null) {
            LOGGER.warning("historico mensaje=MAL_FORMADO tipo=" + tipo);
            return;
        }
        try {
            switch (tipo) {
                case PublicadorHistorico.VIAJE -> historicoDAO.guardarViaje(PublicadorHistorico.JSON.fromJson(cuerpo, ViajeHistoricoDto.class));
                case PublicadorHistorico.CASO -> historicoDAO.guardarCaso(PublicadorHistorico.JSON.fromJson(cuerpo, CasoHistoricoDto.class));
                default -> LOGGER.warning("historico mensaje=TIPO_DESCONOCIDO tipo=" + tipo);
            }
        } catch (JsonbException e) {
            LOGGER.warning("historico mensaje=MAL_FORMADO tipo=" + tipo + " error=" + e);
        }
    }
}
