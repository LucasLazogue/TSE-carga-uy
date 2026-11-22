package tse.cargauy.negocio.historico;

import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import tse.cargauy.historico.HistoricoDAOLocal;

// completa el historico con lo que no llego por la cola: lo anterior a desplegarlo, los datos de prueba y lo que
// termino en la DLQ. Es idempotente, asi que puede correr en cada instancia y cada vez
@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ReconstructorHistorico {

    static final long PRIMERA_MS = 60000;
    static final long CADA_MS = 15 * 60000;

    private static final Logger LOGGER = Logger.getLogger(ReconstructorHistorico.class.getName());

    @EJB
    HistoricoDAOLocal historicoDAO;

    @Resource
    TimerService timers;

    @PostConstruct
    void programar() {
        timers.createIntervalTimer(PRIMERA_MS, CADA_MS, new TimerConfig(null, false));
    }

    @Timeout
    void reconstruir() {
        try {
            int viajes = historicoDAO.agregarViajesFaltantes();
            int casos = historicoDAO.actualizarCasos();
            LOGGER.info("historico reconstruccion viajes=" + viajes + " casos=" + casos);
        } catch (RuntimeException e) {
            LOGGER.warning("historico reconstruccion=FALLIDA error=" + e);
        }
    }
}
