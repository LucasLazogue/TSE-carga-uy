package tse.cargauy.web.ws.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoInfraccion;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.web.ws.ApiEnMemoria;
import tse.cargauy.web.ws.CasosEJBFalso;

class CasosRestTest {

    private final CasosEJBFalso ejb = new CasosEJBFalso();
    private ApiEnMemoria api;

    @BeforeEach
    void preparar() {
        CasosRest rest = new CasosRest();
        rest.casoEJB = ejb;
        api = new ApiEnMemoria(rest);
    }

    @Test
    void listarConFiltros() throws Exception {
        MockHttpResponse respuesta = api.get("/casos?estado=EN_DESCARGO&tipo=EXCESO_CARGA&empresa=10&pagina=1&tamanio=5");

        assertEquals(200, respuesta.getStatus());
        assertEquals(EstadoCaso.EN_DESCARGO, ejb.ultimoFiltro.getEstado());
        assertEquals(TipoInfraccion.EXCESO_CARGA, ejb.ultimoFiltro.getTipo());
        assertEquals(List.of(10L), ejb.ultimoFiltro.getIdsEmpresa());
        assertEquals(1, ejb.ultimaPaginacion.getPagina());
        assertEquals(5, ejb.ultimaPaginacion.getTamanio());
    }

    @Test
    void verCaso() throws Exception {
        MockHttpResponse respuesta = api.get("/casos/3");

        assertEquals(200, respuesta.getStatus());
        assertEquals(3L, ejb.ultimoId);
    }

    @Test
    void casoInexistente() throws Exception {
        ejb.error = new CargaUYException(CodigoError.CASO_NO_ENCONTRADO);

        MockHttpResponse respuesta = api.get("/casos/99");

        assertEquals(404, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("9001"));
    }

    @Test
    void resolver() throws Exception {
        MockHttpResponse respuesta = api.post("/casos/3/resolucion", "{\"resultado\":\"CONFIRMADA\",\"fundamento\":\"pesaje verificado\"}");

        assertEquals(201, respuesta.getStatus());
        assertEquals(3L, ejb.ultimoId);
        assertEquals(EstadoCaso.CONFIRMADA, ejb.ultimaResolucion.getResultado());
        assertEquals("pesaje verificado", ejb.ultimaResolucion.getFundamento());
    }

    @Test
    void resolverConPlazoVigente() throws Exception {
        ejb.error = new CargaUYException(CodigoError.CASO_PLAZO_NO_VENCIDO);

        MockHttpResponse respuesta = api.post("/casos/4/resolucion", "{\"resultado\":\"CONFIRMADA\",\"fundamento\":\"x\"}");

        assertEquals(409, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("9004"));
    }
}
