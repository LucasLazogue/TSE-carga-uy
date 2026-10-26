package tse.cargauy.negocio.balanzas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import tse.cargauy.adaptadores.balanzas.BalanzasClient;
import tse.cargauy.data.nodo.NodoPerifericoDAOLocal;
import tse.cargauy.data.pesaje.PesajeDAOLocal;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.PesajeDto;
import tse.cargauy.dtos.PesajesViajeDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.seguridad.Cifrado;

// lo usa el motor de fiscalizacion en segundo plano, sin usuario. cada pesaje se guarda en su propia transaccion:
// si otra instancia del central ya lo guardo, se sigue con los demas
@Stateless
@PermitAll
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BalanzasEJB implements BalanzasEJBLocal {

    private static final Logger LOGGER = Logger.getLogger(BalanzasEJB.class.getName());

    @EJB
    NodoPerifericoDAOLocal nodoDAO;

    @EJB
    PesajeDAOLocal pesajeDAO;

    @Inject
    BalanzasClient client;

    @Inject
    Cifrado cifrado;

    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    ExecutorService ejecutor;

    public PesajesViajeDto getPesajesDelViaje(ViajeDto viaje) {
        LocalDateTime inicio = viaje.getFechaInicio();
        LocalDateTime fin = viaje.getFechaFin() == null ? ahora() : viaje.getFechaFin();
        // a cada nodo se le pregunta en paralelo: si uno tarda o esta caido no demora a los demas
        PesajesViajeDto resultado = new PesajesViajeDto();
        // cada nodo con la "promesa" de su respuesta, para saber despues que respuesta es de quien
        Map<NodoPerifericoDto, CompletableFuture<List<PesajeDto>>> consultas = new LinkedHashMap<>();
        for (NodoPerifericoDto nodo : nodoDAO.getHabilitados(TipoNodo.BALANZAS)) {
            try {
                String clave = cifrado.descifrar(nodoDAO.getClaveCifrada(nodo.getId()), nodo.getIdentificador());
                // supplyAsync larga la consulta en otro hilo del servidor y sigue sin esperarla
                consultas.put(nodo, CompletableFuture.supplyAsync(
                        () -> consultar(nodo, clave, viaje.getMatricula(), inicio.toLocalDate(), fin.toLocalDate()), ejecutor));
            } catch (CargaUYException e) {
                LOGGER.warning("balanzas consulta=FALLIDA nodo=" + nodo.getIdentificador() + " error=" + e.getCodigo());
                resultado.getNodosSinResponder().add(nodo.getIdentificador());
            }
        }
        for (Map.Entry<NodoPerifericoDto, CompletableFuture<List<PesajeDto>>> consulta : consultas.entrySet()) {
            try {
                // join espera a que ese nodo termine; si fallo, tira CompletionException
                for (PesajeDto pesaje : consulta.getValue().join()) {
                    boolean enElViaje = !pesaje.getFechaHora().isBefore(inicio) && !pesaje.getFechaHora().isAfter(fin);
                    guardar(pesaje, viaje.getIdVehiculo(), enElViaje ? viaje.getId() : null);
                    if (enElViaje) {
                        resultado.getPesajes().add(pesaje);
                    }
                }
            } catch (CompletionException e) {
                LOGGER.warning("balanzas consulta=FALLIDA nodo=" + consulta.getKey().getIdentificador()
                        + " viaje=" + viaje.getId() + " error=" + e.getCause());
                resultado.getNodosSinResponder().add(consulta.getKey().getIdentificador());
            }
        }
        return resultado;
    }

    // la consulta al nodo es por dia: un viaje que cruza la medianoche pregunta por cada fecha
    private List<PesajeDto> consultar(NodoPerifericoDto nodo, String clave, String matricula, LocalDate desde, LocalDate hasta) {
        List<PesajeDto> pesajes = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            pesajes.addAll(client.getPesajes(nodo, clave, matricula, fecha));
        }
        return pesajes;
    }

    private void guardar(PesajeDto pesaje, Long idVehiculo, Long idViaje) {
        pesaje.setIdVehiculo(idVehiculo);
        pesaje.setIdViaje(idViaje);
        pesaje.setRecibidoEn(ahora());
        if (pesajeDAO.existe(pesaje.getIdOrigen())) {
            return;
        }
        try {
            pesajeDAO.registrar(pesaje);
        } catch (CargaUYException e) {
            if (e.getCodigo() != CodigoError.PESAJE_YA_REGISTRADO) {
                throw e;
            }
        }
    }

    LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
