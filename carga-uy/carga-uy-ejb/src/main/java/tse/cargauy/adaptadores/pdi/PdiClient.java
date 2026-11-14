package tse.cargauy.adaptadores.pdi;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import tse.cargauy.adaptadores.pdi.soap.Mensaje;
import tse.cargauy.adaptadores.pdi.soap.ObjPersona;
import tse.cargauy.adaptadores.pdi.soap.ParamObtPersonaPorDoc;
import tse.cargauy.adaptadores.pdi.soap.ResultObtPersonaPorDoc;
import tse.cargauy.adaptadores.pdi.soap.ServicioBasicoInformacion;
import tse.cargauy.adaptadores.pdi.soap.ServicioBasicoInformacionPortType;

// cliente SOAP del Servicio Basico de Informacion de DNIC. los logs no llevan la cedula ni la clave
@ApplicationScoped
public class PdiClient {

    private static final Logger LOGGER = Logger.getLogger(PdiClient.class.getName());

    private static final String WSDL = "/wsdl/pdi/ServicioBasicoInformacion.wsdl";
    private static final String TIPO_CEDULA = "DO";
    private static final int ESPERA_MS = 3000;
    // @Timeout interrumpe el hilo, pero la lectura del socket la corta CXF con estas dos propiedades
    private static final String ESPERA_CONEXION = "jakarta.xml.ws.client.connectionTimeout";
    private static final String ESPERA_RESPUESTA = "jakarta.xml.ws.client.receiveTimeout";

    private static final int PERSONA_INEXISTENTE = 500;
    private static final int CONSULTA_INCOMPLETA = 1001;
    private static final int LIMITE_EXCEDIDO = 1002;
    private static final int CEDULA_ANULADA = 1003;

    @Timeout(ESPERA_MS)
    @Retry(maxRetries = 1, delay = 1000, jitter = 0, abortOn = CircuitBreakerOpenException.class)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    @Fallback(fallbackMethod = "sinDatos")
    public Optional<PersonaPdi> obtenerPersona(String cedula) {
        String url = entorno("PDI_URL");
        if (url == null || url.isBlank()) {
            LOGGER.warning("Falta configurar PDI_URL: el perfil queda sin los datos de la PDI");
            return Optional.empty();
        }
        ParamObtPersonaPorDoc parametros = new ParamObtPersonaPorDoc();
        parametros.setOrganizacion(entorno("PDI_ORGANIZACION"));
        parametros.setPasswordEntidad(entorno("PDI_PASSWORD_ENTIDAD"));
        parametros.setNroDocumento(cedula);
        parametros.setTipoDocumento(TIPO_CEDULA);
        return interpretar(puerto(url).obtPersonaPorDoc(parametros));
    }

    Optional<PersonaPdi> sinDatos(String cedula) {
        LOGGER.warning("La PDI no esta disponible: el perfil queda pendiente hasta el proximo ingreso");
        return Optional.empty();
    }

    // aparte para poder reemplazarlo en los tests
    String entorno(String variable) {
        return System.getenv(variable);
    }

    // el WSDL sale del classpath para que el central arranque y funcione con la PDI caida. un puerto por llamada porque
    // su contexto no es seguro entre hilos; alcanza con esto, solo se llama en los ingresos con el perfil pendiente
    private static ServicioBasicoInformacionPortType puerto(String url) {
        ServicioBasicoInformacionPortType puerto = new ServicioBasicoInformacion(PdiClient.class.getResource(WSDL))
                .getServicioBasicoInformacionPort();
        Map<String, Object> contexto = ((BindingProvider) puerto).getRequestContext();
        contexto.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        contexto.put(ESPERA_CONEXION, ESPERA_MS);
        contexto.put(ESPERA_RESPUESTA, ESPERA_MS);
        return puerto;
    }

    private static Optional<PersonaPdi> interpretar(ResultObtPersonaPorDoc respuesta) {
        List<Mensaje> errores = respuesta.getErrores() == null ? List.of() : respuesta.getErrores().getMensajes();
        if (!errores.isEmpty()) {
            return sinDatosPorError(errores.get(0).getCodMensaje());
        }
        ObjPersona persona = respuesta.getObjPersona();
        if (persona == null) {
            return Optional.empty();
        }
        return Optional.of(new PersonaPdi(
                unir(persona.getNombre1(), persona.getNombre2()),
                unir(persona.getApellido1(), persona.getApellido2()),
                FechaDnic.normalizar(persona.getFechaNacimiento())));
    }

    private static Optional<PersonaPdi> sinDatosPorError(int codigo) {
        if (codigo == CONSULTA_INCOMPLETA || codigo == LIMITE_EXCEDIDO) {
            // no es una respuesta definitiva: tiene que reintentarse y contar para el corte, como una falla de red
            throw new WebServiceException("La PDI no pudo completar la consulta: codigo " + codigo);
        }
        if (codigo != PERSONA_INEXISTENTE && codigo != CEDULA_ANULADA) {
            LOGGER.severe("La PDI rechazo la consulta, revisar PDI_ORGANIZACION y PDI_PASSWORD_ENTIDAD: codigo " + codigo);
        }
        return Optional.empty();
    }

    private static String unir(String primero, String segundo) {
        String unido = Stream.of(primero, segundo)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(parte -> !parte.isEmpty())
                .collect(Collectors.joining(" "));
        return unido.isEmpty() ? null : unido;
    }
}
