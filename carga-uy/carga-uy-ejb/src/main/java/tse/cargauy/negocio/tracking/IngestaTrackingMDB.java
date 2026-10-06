package tse.cargauy.negocio.tracking;

import java.io.StringReader;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import tse.cargauy.dtos.EventoPosicionDto;

// formato del mensaje (JSON en un TextMessage):
// {"idEvento":"...","nodo":"...","matricula":"...","latitud":-34.9,"longitud":-56.1,"timestampGeneracion":"2026-10-06T12:00:00Z"}
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/queue_tracking"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class IngestaTrackingMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(IngestaTrackingMDB.class.getName());

    @EJB
    IngestaTrackingEJBLocal ingesta;

    @Override
    public void onMessage(Message message) {
        EventoPosicionDto evento;
        try {
            evento = leer(message);
        } catch (JMSException | JsonException | ClassCastException | NullPointerException | DateTimeParseException e) {
            // un mensaje mal formado no se arregla reintentando: se descarta para no trancar la cola
            LOGGER.warning("ingesta resultado=MAL_FORMADO error=" + e);
            return;
        }
        // si falla otra cosa (por ejemplo la base) la excepcion sigue de largo y el broker reentrega el mensaje
        ResultadoIngesta resultado = ingesta.procesar(evento);
        LOGGER.info("ingesta resultado=" + resultado + " evento=" + evento.getIdEvento()
                + " nodo=" + evento.getNodo() + " matricula=" + evento.getMatricula());
    }

    private static EventoPosicionDto leer(Message message) throws JMSException {
        if (!(message instanceof TextMessage texto)) {
            throw new JsonException("el mensaje no es de texto");
        }
        JsonObject json = Json.createReader(new StringReader(texto.getText())).readObject();
        EventoPosicionDto evento = new EventoPosicionDto();
        evento.setIdEvento(json.getString("idEvento"));
        evento.setNodo(json.getString("nodo"));
        evento.setMatricula(json.getString("matricula"));
        evento.setLatitud(json.getJsonNumber("latitud").doubleValue());
        evento.setLongitud(json.getJsonNumber("longitud").doubleValue());
        evento.setTimestampGeneracion(Instant.parse(json.getString("timestampGeneracion")));
        return evento;
    }
}
