package tse.cargauy.negocio.balanzas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import io.opentelemetry.context.Context;
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
    // cuanto se espera, en total, a que respondan todos los nodos. Cada consulta ya tiene su timeout y su reintento
    // (BalanzasClient), pero puede esperar en la cola del ejecutor detras de las de un nodo colgado: el tope acota
    // lo que tarda el pedido pase lo que pase. El nodo que no respondio a tiempo queda como sin responder
    static final long TOPE_MS = 7000;

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
        PesajesViajeDto resultado = new PesajesViajeDto();
        // un viaje asignado que todavia no empezo no tiene pesajes: no se le pregunta a nadie y la respuesta esta
        // completa. Antes fallaba al armar la consulta y los nodos figuraban como si no hubieran respondido
        if (inicio == null) {
            return resultado;
        }
        LocalDateTime fin = viaje.getFechaFin() == null ? ahora() : viaje.getFechaFin();
        // a cada nodo se le pregunta en paralelo: si uno tarda o esta caido no demora a los demas
        // cada nodo con la "promesa" de su respuesta, para saber despues que respuesta es de quien
        Map<NodoPerifericoDto, CompletableFuture<List<PesajeDto>>> consultas = new LinkedHashMap<>();
        // los hilos del ejecutor no heredan la traza en curso: sin esto cada consulta a un nodo queda como una traza
        // suelta en vez de colgar de la evaluacion del viaje
        Executor conTraza = Context.current().wrap(ejecutor);
        for (NodoPerifericoDto nodo : nodoDAO.getHabilitados(TipoNodo.BALANZAS)) {
            try {
                String clave = cifrado.descifrar(nodoDAO.getClaveCifrada(nodo.getId()), nodo.getIdentificador());
                // supplyAsync larga la consulta en otro hilo del servidor y sigue sin esperarla
                consultas.put(nodo, CompletableFuture.supplyAsync(
                        () -> consultar(nodo, clave, viaje.getMatricula(), inicio.toLocalDate(), fin.toLocalDate()), conTraza));
            } catch (CargaUYException e) {
                LOGGER.warning("balanzas consulta=FALLIDA nodo=" + nodo.getIdentificador() + " error=" + e.getCodigo());
                resultado.getNodosSinResponder().add(nodo.getIdentificador());
            } catch (RejectedExecutionException e) {
                LOGGER.warning("balanzas consulta=FALLIDA nodo=" + nodo.getIdentificador() + " error=ejecutor lleno");
                resultado.getNodosSinResponder().add(nodo.getIdentificador());
            }
        }
        long vence = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(tope());
        for (Map.Entry<NodoPerifericoDto, CompletableFuture<List<PesajeDto>>> consulta : consultas.entrySet()) {
            try {
                // get espera a que ese nodo termine, como mucho hasta el tope; si fallo, tira ExecutionException
                long resto = Math.max(0, vence - System.nanoTime());
                for (PesajeDto pesaje : consulta.getValue().get(resto, TimeUnit.NANOSECONDS)) {
                    boolean enElViaje = !pesaje.getFechaHora().isBefore(inicio) && !pesaje.getFechaHora().isAfter(fin);
                    guardar(pesaje, viaje.getIdVehiculo(), enElViaje ? viaje.getId() : null);
                    if (enElViaje) {
                        resultado.getPesajes().add(pesaje);
                    }
                }
            } catch (ExecutionException e) {
                LOGGER.warning("balanzas consulta=FALLIDA nodo=" + consulta.getKey().getIdentificador()
                        + " viaje=" + viaje.getId() + " error=" + e.getCause());
                resultado.getNodosSinResponder().add(consulta.getKey().getIdentificador());
            } catch (TimeoutException e) {
                // si la consulta todavia estaba en la cola del ejecutor, cancelada ya no corre
                consulta.getValue().cancel(true);
                LOGGER.warning("balanzas consulta=FALLIDA nodo=" + consulta.getKey().getIdentificador()
                        + " viaje=" + viaje.getId() + " error=sin respuesta en " + tope() + " ms");
                resultado.getNodosSinResponder().add(consulta.getKey().getIdentificador());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                consulta.getValue().cancel(true);
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

    // package-private para que los tests usen un tope corto
    long tope() {
        return TOPE_MS;
    }

    LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
