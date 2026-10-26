package tse.cargauy.web.ws.infracciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void listarConFiltros() throws Exception {
        MockHttpResponse respuesta = api.get("/infracciones?empresa=10&estado=NOTIFICADA");

        assertEquals(200, respuesta.getStatus());
        assertEquals(10L, ejb.ultimaEmpresa);
        assertEquals(EstadoCaso.NOTIFICADA, ejb.ultimoEstado);
    }

    @Test
    void verInfraccion() throws Exception {
        MockHttpResponse respuesta = api.get("/infracciones/1");

        assertEquals(200, respuesta.getStatus());
        assertEquals(1L, ejb.ultimoId);
    }

    @Test
    void presentarDescargo() throws Exception {
        MockHttpResponse respuesta = api.post("/infracciones/1/descargo", "{\"texto\":\"la balanza estaba descalibrada\"}");

        assertEquals(201, respuesta.getStatus());
        assertEquals(1L, ejb.ultimoId);
        assertEquals("la balanza estaba descalibrada", ejb.ultimoDescargo.getTexto());
    }

    @Test
    void descargoFueraDePlazo() throws Exception {
        ejb.error = new CargaUYException(CodigoError.DESCARGO_FUERA_DE_PLAZO);

        MockHttpResponse respuesta = api.post("/infracciones/2/descargo", "{\"texto\":\"tarde\"}");

        assertEquals(409, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("9008"));
    }

    @Test
    void marcarNotificacionesLeidas() throws Exception {
        MockHttpResponse respuesta = api.post("/infracciones/notificaciones/lectura", "");

        assertEquals(204, respuesta.getStatus());
        assertTrue(ejb.leyoEmpresa);
    }
}
