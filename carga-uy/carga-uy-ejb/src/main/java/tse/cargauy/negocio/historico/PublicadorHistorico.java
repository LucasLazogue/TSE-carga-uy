package tse.cargauy.negocio.historico;

import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import tse.cargauy.data.informe.InformeDAOLocal;
import tse.cargauy.dtos.CasoHistoricoDto;
import tse.cargauy.dtos.ViajeHistoricoDto;
import tse.cargauy.negocio.fiscalizacion.CasoCambiado;
import tse.cargauy.negocio.viaje.ViajeIniciadoOFinalizado;

// avisa recien cuando se confirmo el cambio, para que la operativa no dependa del broker. Si el aviso no sale,
// ReconstructorHistorico lo completa en su proxima pasada. La transaccion del cambio ya esta confirmada: cada
// consulta usa la suya
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PublicadorHistorico {

    static final String TIPO = "tipo";
    static final String VIAJE = "viaje";
    static final String CASO = "caso";

    static final Jsonb JSON = JsonbBuilder.create();

    private static final Logger LOGGER = Logger.getLogger(PublicadorHistorico.class.getName());

    @EJB
    InformeDAOLocal informeDAO;

    @Inject
    @JMSConnectionFactory("java:/jms/ArtemisCF")
    JMSContext jms;

    @Resource(lookup = "java:/jms/queue/queue_historico")
    Queue cola;

    public void alCambiarViaje(@Observes(during = TransactionPhase.AFTER_SUCCESS) ViajeIniciadoOFinalizado cambio) {
        try {
            ViajeHistoricoDto viaje = informeDAO.getViajeHistorico(cambio.idViaje());
            if (viaje != null) {
                enviar(VIAJE, JSON.toJson(viaje));
            }
        } catch (RuntimeException e) {
            LOGGER.warning("historico aviso=FALLIDO viaje=" + cambio.idViaje() + " error=" + e);
        }
    }

    public void alCambiarCaso(@Observes(during = TransactionPhase.AFTER_SUCCESS) CasoCambiado cambio) {
        try {
            CasoHistoricoDto caso = informeDAO.getCasoHistorico(cambio.idCaso());
            if (caso != null) {
                enviar(CASO, JSON.toJson(caso));
            }
        } catch (RuntimeException e) {
            LOGGER.warning("historico aviso=FALLIDO caso=" + cambio.idCaso() + " error=" + e);
        }
    }

    void enviar(String tipo, String cuerpo) {
        jms.createProducer().setProperty(TIPO, tipo).send(cola, cuerpo);
    }
}
