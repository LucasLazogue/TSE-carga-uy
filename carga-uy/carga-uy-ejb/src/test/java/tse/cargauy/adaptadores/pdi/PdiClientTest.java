package tse.cargauy.adaptadores.pdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sun.net.httpserver.HttpServer;

import jakarta.xml.ws.WebServiceException;

// sin contenedor las anotaciones de tolerancia a fallas no hacen nada: aca se prueba el mensaje SOAP y como se
// interpreta la respuesta. la espera, los reintentos y el corte se prueban con el compose
class PdiClientTest {

    private static final String SOBRE = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>%s</soap:Body></soap:Envelope>
            """;
    private static final String NS = "urn:carga-uy:suplantador-pdi:dnic:servicio-basico-informacion";
    private static final String PERSONA = """
            <objPersona>
              <codTipoDocumento>DO</codTipoDocumento><nroDocumento>22222222</nroDocumento>
              <nombre1>Ana</nombre1><nombre2>María</nombre2><Apellido1>Pereira</Apellido1><Apellido2>Gómez</Apellido2>
              <sexo>2</sexo><fechaNacimiento>1975-1103</fechaNacimiento><codNacionalidad>1</codNacionalidad>
            </objPersona>
            """;

    private final Map<String, String> variables = new HashMap<>();
    private final PdiClient client = new PdiClient() {
        @Override
        String entorno(String variable) {
            return variables.get(variable);
        }
    };

    private HttpServer pdi;
    private int status;
    private String respuesta;
    private String pedido;
    private String accion;

    @BeforeEach
    void levantarPdi() throws IOException {
        status = 200;
        pdi = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        pdi.createContext("/pdi", exchange -> {
            pedido = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            accion = exchange.getRequestHeaders().getFirst("SOAPAction");
            byte[] cuerpo = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/xml; charset=utf-8");
            exchange.sendResponseHeaders(status, cuerpo.length);
            try (OutputStream salida = exchange.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        pdi.start();
        variables.put("PDI_URL", "http://localhost:" + pdi.getAddress().getPort() + "/pdi");
        variables.put("PDI_ORGANIZACION", "carga-uy");
        variables.put("PDI_PASSWORD_ENTIDAD", "la-clave");
    }

    @AfterEach
    void bajarPdi() {
        pdi.stop(0);
    }

    private void responder(String contenido) {
        respuesta = SOBRE.formatted(
                "<resultObtPersonaPorDoc xmlns=\"" + NS + "\">" + contenido + "</resultObtPersonaPorDoc>");
    }

    private void responderError(int codigo) {
        responder("<Errores><Mensajes><CodMensaje>" + codigo + "</CodMensaje><Descripcion>d</Descripcion>"
                + "<DatoExtra>t</DatoExtra></Mensajes></Errores>");
    }

    @Test
    void mandaLosParametrosDelContrato() {
        responder(PERSONA);

        client.obtenerPersona("22222222");

        assertTrue(pedido.contains("paramobtPersonaPorDoc"), pedido);
        assertTrue(pedido.contains(">carga-uy</"), pedido);
        assertTrue(pedido.contains(">la-clave</"), pedido);
        assertTrue(pedido.contains("NroDocumento"), pedido);
        assertTrue(pedido.contains(">22222222</"), pedido);
        assertTrue(pedido.contains("TipoDocumento"), pedido);
        assertTrue(pedido.contains(">DO</"), pedido);
        assertNotNull(accion);
        assertTrue(accion.contains("ObtPersonaPorDoc"), accion);
    }

    @Test
    void armaNombresYApellidosYNormalizaLaFecha() {
        responder(PERSONA);

        PersonaPdi persona = client.obtenerPersona("22222222").orElseThrow();

        assertEquals("Ana María", persona.nombres());
        assertEquals("Pereira Gómez", persona.apellidos());
        assertEquals("1975-11-03", persona.fechaNacimiento());
    }

    @Test
    void sinSegundoNombreNiFechaNoInventaNada() {
        responder("<objPersona><nombre1>Jorge</nombre1><Apellido1>Silva</Apellido1>"
                + "<fechaNacimiento></fechaNacimiento></objPersona>");

        PersonaPdi persona = client.obtenerPersona("44444444").orElseThrow();

        assertEquals("Jorge", persona.nombres());
        assertEquals("Silva", persona.apellidos());
        assertNull(persona.fechaNacimiento());
    }

    @Test
    void losCamposVaciosNoDejanEspacios() {
        responder("<objPersona><nombre1>Jorge</nombre1><nombre2></nombre2>"
                + "<Apellido1>Silva</Apellido1><Apellido2> </Apellido2></objPersona>");

        PersonaPdi persona = client.obtenerPersona("44444444").orElseThrow();

        assertEquals("Jorge", persona.nombres());
        assertEquals("Silva", persona.apellidos());
    }

    // nombres null marca el perfil como pendiente: con "" quedaria completo y sin nombre para mostrar
    @Test
    void sinNombresElPerfilQuedaPendiente() {
        responder("<objPersona><nombre1> </nombre1><Apellido1>Silva</Apellido1>"
                + "<fechaNacimiento>1975-1103</fechaNacimiento></objPersona>");

        PersonaPdi persona = client.obtenerPersona("44444444").orElseThrow();

        assertNull(persona.nombres());
        assertEquals("1975-11-03", persona.fechaNacimiento());
    }

    @Test
    void laAdvertenciaDeDatosARegularizarIgualCompletaElPerfil() {
        responder(PERSONA + "<Warnings><Mensajes><CodMensaje>701</CodMensaje></Mensajes></Warnings>");

        assertEquals("Ana María", client.obtenerPersona("66666666").orElseThrow().nombres());
    }

    @Test
    void unaRespuestaVaciaEsSinDatos() {
        responder("");

        assertEquals(Optional.empty(), client.obtenerPersona("22222222"));
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 1003, 10001, 10002})
    void losRechazosDefinitivosSonSinDatos(int codigo) {
        responderError(codigo);

        assertEquals(Optional.empty(), client.obtenerPersona("22222222"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1001, 1002})
    void losErroresTransitoriosSonFallasParaReintentar(int codigo) {
        responderError(codigo);

        assertThrows(WebServiceException.class, () -> client.obtenerPersona("22222222"));
    }

    @Test
    void unSoapFaultEsUnaFalla() {
        status = 500;
        respuesta = SOBRE.formatted("<soap:Fault><faultcode>soap:Server</faultcode>"
                + "<faultstring>error</faultstring></soap:Fault>");

        assertThrows(WebServiceException.class, () -> client.obtenerPersona("22222222"));
    }

    @Test
    void siLaPdiNoRespondeEsUnaFalla() {
        pdi.stop(0);

        assertThrows(WebServiceException.class, () -> client.obtenerPersona("22222222"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void sinUrlConfiguradaEsSinDatosYNoLlama(String url) {
        variables.put("PDI_URL", url);
        responder(PERSONA);

        assertEquals(Optional.empty(), client.obtenerPersona("22222222"));
        assertNull(pedido);
    }

    @Test
    void elFallbackEsSinDatos() {
        assertEquals(Optional.empty(), client.sinDatos("22222222"));
    }
}
