package tse.cargauy.web.ws.infracciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.web.ws.ApiEnMemoria;
import tse.cargauy.web.ws.CasosEJBFalso;

class InfraccionesRestTest {

    private final CasosEJBFalso ejb = new CasosEJBFalso();
    private ApiEnMemoria api;

    @BeforeEach
    void preparar() {
        InfraccionesRest rest = new InfraccionesRest();
        rest.casoEJB = ejb;
        api = new ApiEnMemoria(rest);
    }

    @Test
    void listarPasaEmpresaEstadoYPaginacion() throws Exception {
        MockHttpResponse respuesta = api.get("/infracciones?empresa=10&estado=NOTIFICADA&pagina=2&tamanio=10");

        assertEquals(200, respuesta.getStatus());
        assertEquals("listarInfracciones", ejb.ultimoMetodo);
        assertEquals(10L, ejb.ultimaEmpresa);
        assertEquals(EstadoCaso.NOTIFICADA, ejb.ultimoEstado);
        assertEquals(2, ejb.ultimaPaginacion.getPagina());
        assertEquals(10, ejb.ultimaPaginacion.getTamanio());
    }

    @Test
    void listarSinFiltros() throws Exception {
        MockHttpResponse respuesta = api.get("/infracciones");

        assertEquals(200, respuesta.getStatus());
        assertNull(ejb.ultimaEmpresa);
        assertNull(ejb.ultimoEstado);
    }

    @Test
    void notificacionesNoSeConfundeConElDetalle() throws Exception {
        MockHttpResponse respuesta = api.get("/infracciones/notificaciones");

        assertEquals(200, respuesta.getStatus());
        assertEquals("getNotificaciones", ejb.ultimoMetodo);
        assertTrue(respuesta.getContentAsString().contains("\"idCaso\":1"));
    }

    @Test
    void detalleDeLaInfraccion() throws Exception {
        MockHttpResponse respuesta = api.get("/infracciones/1");

        assertEquals(200, respuesta.getStatus());
        assertEquals("getInfraccion", ejb.ultimoMetodo);
        assertEquals(1L, ejb.ultimoId);
    }

    @Test
    void infraccionDeOtraEmpresaDevuelve403() throws Exception {
        ejb.error = new CargaUYException(CodigoError.ACCESO_DENEGADO);

        MockHttpResponse respuesta = api.get("/infracciones/4");

        assertEquals(403, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("\"codigo\":8003"));
    }

    @Test
    void presentarDescargoDevuelve201() throws Exception {
        MockHttpResponse respuesta = api.post("/infracciones/1/descargo", "{\"texto\":\"la balanza estaba descalibrada\"}");

        assertEquals(201, respuesta.getStatus());
        assertEquals(1L, ejb.ultimoId);
        assertEquals("la balanza estaba descalibrada", ejb.ultimoDescargo.getTexto());
        assertTrue(respuesta.getContentAsString().contains("\"estado\":\"EN_DESCARGO\""));
    }

    @Test
    void descargoFueraDePlazoDevuelve409() throws Exception {
        ejb.error = new CargaUYException(CodigoError.DESCARGO_FUERA_DE_PLAZO);

        MockHttpResponse respuesta = api.post("/infracciones/2/descargo", "{\"texto\":\"tarde\"}");

        assertEquals(409, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("\"codigo\":9008"));
    }

    @Test
    void segundoDescargoDevuelve409() throws Exception {
        ejb.error = new CargaUYException(CodigoError.DESCARGO_YA_PRESENTADO);

        MockHttpResponse respuesta = api.post("/infracciones/3/descargo", "{\"texto\":\"otro\"}");

        assertEquals(409, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("\"codigo\":9009"));
    }

    @Test
    void descargoVacioDevuelve400() throws Exception {
        ejb.error = new CargaUYException(CodigoError.DESCARGO_TEXTO_REQUERIDO);

        MockHttpResponse respuesta = api.post("/infracciones/1/descargo", "{\"texto\":\"\"}");

        assertEquals(400, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("\"codigo\":9007"));
    }
}
