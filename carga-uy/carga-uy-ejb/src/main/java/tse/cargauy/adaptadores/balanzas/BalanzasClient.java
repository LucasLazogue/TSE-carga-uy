package tse.cargauy.adaptadores.balanzas;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import jakarta.enterprise.context.ApplicationScoped;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.PesajeDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@ApplicationScoped
public class BalanzasClient {

    private final Map<String, NodoBalanzasApi> nodos = new ConcurrentHashMap<>();

    // cada consulta espera hasta 5 s y si falla se reintenta a 1 s y a 2 s. si fallan 5 de las ultimas 10 se corta
    // 60 s: en ese tiempo se responde "no disponible" al instante sin llamar a los nodos.
    // el corte es uno solo para todas las balanzas (las anotaciones guardan un estado por metodo, no por nodo):
    // si se cae un nodo tambien se deja de preguntar a los demas, pero la evaluacion de exceso de carga necesita
    // los pesajes de todos los nodos y queda pendiente igual.
    // los rechazos del nodo (CargaUYException) no se reintentan ni cuentan para el corte: no se arreglan solos
    @Timeout(5000)
    @Retry(maxRetries = 2, delay = 1000, jitter = 0, abortOn = {CargaUYException.class, CircuitBreakerOpenException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 60000, skipOn = CargaUYException.class)
    @Fallback(fallbackMethod = "noDisponible", skipOn = CargaUYException.class)
    public List<PesajeDto> getPesajes(NodoPerifericoDto nodo, String clave, String matricula, LocalDate fecha) {
        return api(nodo.getPuntoAcceso()).getPesajes("Bearer " + clave, matricula, fecha).stream()
                .map(pesaje -> toDto(pesaje, nodo))
                .toList();
    }

    List<PesajeDto> noDisponible(NodoPerifericoDto nodo, String clave, String matricula, LocalDate fecha) {
        throw new CargaUYException(CodigoError.BALANZAS_NO_DISPONIBLE);
    }

    private NodoBalanzasApi api(String puntoAcceso) {
        return nodos.computeIfAbsent(puntoAcceso,
                url -> RestClientBuilder.newBuilder().baseUri(URI.create(url)).build(NodoBalanzasApi.class));
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
