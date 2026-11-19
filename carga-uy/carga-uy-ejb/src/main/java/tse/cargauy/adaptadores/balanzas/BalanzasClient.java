package tse.cargauy.adaptadores.balanzas;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.PesajeDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@ApplicationScoped
public class BalanzasClient {

    private final Map<String, NodoBalanzasApi> nodos = new ConcurrentHashMap<>();

    // un corte por nodo (CortePorNodo): si se cae uno se le sigue preguntando a los demas
    private final CortePorNodo corte = new CortePorNodo();

    // lo pone WildFly; en los tests sin contenedor queda en null y no se publican metricas
    @Inject
    MeterRegistry registro;

    // cada consulta espera hasta 3 s y si falla se reintenta una vez a los 0,5 s: 6,5 s en el peor caso, dentro del
    // tope de BalanzasEJB. si fallan 5 de las ultimas 10 llamadas a un nodo, ese nodo se corta 60 s: en ese tiempo se
    // responde "no disponible" al instante sin llamarlo. Cada intento cuenta para el corte, como con @CircuitBreaker.
    // los rechazos del nodo (CargaUYException) no se reintentan ni cuentan como falla: no se arreglan solos
    @Timeout(3000)
    @Retry(maxRetries = 1, delay = 500, jitter = 0, abortOn = {CargaUYException.class, CircuitBreakerOpenException.class})
    @Fallback(fallbackMethod = "noDisponible", skipOn = CargaUYException.class)
    public List<PesajeDto> getPesajes(NodoPerifericoDto nodo, String clave, String matricula, LocalDate fecha) {
        String id = nodo.getIdentificador();
        publicarEstado(id);
        if (!corte.permite(id)) {
            contar(id, "cortada");
            throw new CircuitBreakerOpenException("corte abierto para el nodo " + id);
        }
        try {
            List<PesajeDto> pesajes = api(nodo.getPuntoAcceso()).getPesajes("Bearer " + clave, matricula, fecha).stream()
                    .map(pesaje -> toDto(pesaje, nodo))
                    .toList();
            corte.exito(id);
            contar(id, "exito");
            return pesajes;
        } catch (CargaUYException e) {
            corte.exito(id);
            contar(id, "rechazo");
            throw e;
        } catch (RuntimeException e) {
            corte.falla(id);
            contar(id, "falla");
            throw e;
        }
    }

    // estado del corte de cada nodo para el tablero (AC13): 0 cerrado, 0,5 probando, 1 abierto. Uno por instancia
    private void publicarEstado(String id) {
        if (registro != null && !corte.conoce(id)) {
            Gauge.builder("balanzas.corte", corte, c -> c.estado(id).valor)
                    .description("Corte del nodo de balanzas: 0 cerrado, 0,5 probando, 1 abierto")
                    .tag("nodo", id)
                    .register(registro);
        }
    }

    private void contar(String id, String resultado) {
        if (registro != null) {
            registro.counter("balanzas.llamadas", "nodo", id, "resultado", resultado).increment();
        }
    }

    // package-private para los tests
    CortePorNodo corte() {
        return corte;
    }

    List<PesajeDto> noDisponible(NodoPerifericoDto nodo, String clave, String matricula, LocalDate fecha) {
        throw new CargaUYException(CodigoError.BALANZAS_NO_DISPONIBLE);
    }

    // el @Timeout no puede cortar un socket bloqueado: contra un nodo que no contesta, la conexion quedaba esperando
    // el timeout del sistema operativo (minutos) y ocupaba los hilos del ejecutor (docs/performance.md, balanzas
    // caidas). Con estos topes el propio socket se corta y la falla llega a tiempo al reintento y al corte
    static final int CONEXION_MS = 2000;
    static final int LECTURA_MS = 3000;

    private NodoBalanzasApi api(String puntoAcceso) {
        return nodos.computeIfAbsent(puntoAcceso, url -> RestClientBuilder.newBuilder()
                .baseUri(URI.create(url))
                .connectTimeout(CONEXION_MS, TimeUnit.MILLISECONDS)
                .readTimeout(LECTURA_MS, TimeUnit.MILLISECONDS)
                .build(NodoBalanzasApi.class));
    }

    static PesajeDto toDto(PesajeNodo pesaje, NodoPerifericoDto nodo) {
        PesajeDto dto = new PesajeDto();
        dto.setIdOrigen(nodo.getIdentificador() + "/" + pesaje.getBalanza().getCodigo() + "/" + pesaje.getIdRegistro());
        dto.setIdNodo(nodo.getId());
        dto.setMatricula(pesaje.getMatricula());
        dto.setFechaHora(pesaje.getFechaHora());
        dto.setPesoRegistrado(pesaje.getPesoRegistrado());
        dto.setCodigoBalanza(pesaje.getBalanza().getCodigo());
        dto.setSentido(pesaje.getBalanza().getSentido());
        dto.setCodigoPeaje(pesaje.getPeaje().getCodigo());
        dto.setNombrePeaje(pesaje.getPeaje().getNombre());
        dto.setRuta(pesaje.getPeaje().getRuta());
        dto.setKm(pesaje.getPeaje().getKm());
        dto.setLatitud(pesaje.getPeaje().getLatitud());
        dto.setLongitud(pesaje.getPeaje().getLongitud());
        return dto;
    }
}
