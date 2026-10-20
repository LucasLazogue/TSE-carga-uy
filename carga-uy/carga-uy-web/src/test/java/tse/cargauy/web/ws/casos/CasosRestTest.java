package tse.cargauy.web.ws.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
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
    void listarPasaLosFiltrosYLaPaginacion() throws Exception {
        MockHttpResponse respuesta = api.get("/casos?estado=EN_DESCARGO&tipo=EXCESO_CARGA&empresa=10&desde=2026-10-01&hasta=2026-10-20&pagina=1&tamanio=5");

        assertEquals(200, respuesta.getStatus());
        assertEquals("listar", ejb.ultimoMetodo);
        assertEquals(EstadoCaso.EN_DESCARGO, ejb.ultimoFiltro.getEstado());
        assertEquals(TipoInfraccion.EXCESO_CARGA, ejb.ultimoFiltro.getTipo());
        assertEquals(List.of(10L), ejb.ultimoFiltro.getIdsEmpresa());
        assertEquals(LocalDate.of(2026, 10, 1), ejb.ultimoFiltro.getDesde());
        assertEquals(LocalDate.of(2026, 10, 20), ejb.ultimoFiltro.getHasta());
        assertEquals(1, ejb.ultimaPaginacion.getPagina());
        assertEquals(5, ejb.ultimaPaginacion.getTamanio());
        assertTrue(respuesta.getContentAsString().contains("\"items\""));
    }

    @Test
    void listarSinParametrosUsaLaPrimeraPagina() throws Exception {
        MockHttpResponse respuesta = api.get("/casos");

        assertEquals(200, respuesta.getStatus());
        assertNull(ejb.ultimoFiltro.getEstado());
        assertNull(ejb.ultimoFiltro.getIdsEmpresa());
        assertEquals(0, ejb.ultimaPaginacion.getPagina());
        assertEquals(20, ejb.ultimaPaginacion.getTamanio());
    }

    @Test
    void tamanioDePaginaFueraDeRangoDevuelve400() throws Exception {
        MockHttpResponse respuesta = api.get("/casos?tamanio=500");

        assertEquals(400, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains(String.valueOf(CodigoError.PAGINACION_INVALIDA.getCodigo())));
    }

    @Test
    void estadoDesconocidoDevuelve404() throws Exception {
        MockHttpResponse respuesta = api.get("/casos?estado=ARCHIVADA");

        assertEquals(404, respuesta.getStatus());
        assertNull(ejb.ultimoMetodo);
    }

    @Test
    void fechaVaciaNoFiltra() throws Exception {
        MockHttpResponse respuesta = api.get("/casos?desde=&hasta=");

        assertEquals(200, respuesta.getStatus());
        assertNull(ejb.ultimoFiltro.getDesde());
        assertNull(ejb.ultimoFiltro.getHasta());
    }

    @Test
    void fechaMalFormadaNoLlegaAlEjb() throws Exception {
        MockHttpResponse respuesta = api.get("/casos?desde=13-10-2026");

        assertEquals(404, respuesta.getStatus());
        assertNull(ejb.ultimoMetodo);
    }

    @Test
    void detalleDelCaso() throws Exception {
        MockHttpResponse respuesta = api.get("/casos/3");

        assertEquals(200, respuesta.getStatus());
        assertEquals("getCaso", ejb.ultimoMetodo);
        assertEquals(3L, ejb.ultimoId);
        assertTrue(respuesta.getContentAsString().contains("\"id\":3"));
    }

    @Test
    void casoInexistenteDevuelve404ConSuCodigo() throws Exception {
        ejb.error = new CargaUYException(CodigoError.CASO_NO_ENCONTRADO);

        MockHttpResponse respuesta = api.get("/casos/99");

        assertEquals(404, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("\"codigo\":9001"));
    }

    @Test
    void auditoriaDelCaso() throws Exception {
        MockHttpResponse respuesta = api.get("/casos/3/auditoria");

        assertEquals(200, respuesta.getStatus());
        assertEquals("getAuditoria", ejb.ultimoMetodo);
        assertTrue(respuesta.getContentAsString().contains("\"estadoNuevo\":\"DETECTADA\""));
    }

    @Test
    void resolverDevuelve201() throws Exception {
        MockHttpResponse respuesta = api.post("/casos/3/resolucion", "{\"resultado\":\"CONFIRMADA\",\"fundamento\":\"pesaje verificado\"}");

        assertEquals(201, respuesta.getStatus());
        assertEquals(3L, ejb.ultimoId);
        assertEquals(EstadoCaso.CONFIRMADA, ejb.ultimaResolucion.getResultado());
        assertEquals("pesaje verificado", ejb.ultimaResolucion.getFundamento());
    }

    @Test
    void resolverConTransicionInvalidaDevuelve409() throws Exception {
        ejb.error = new CargaUYException(CodigoError.CASO_TRANSICION_INVALIDA);

        MockHttpResponse respuesta = api.post("/casos/3/resolucion", "{\"resultado\":\"CONFIRMADA\",\"fundamento\":\"x\"}");

        assertEquals(409, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("\"codigo\":9002"));
    }

    @Test
    void resolverSinFundamentoDevuelve400() throws Exception {
        ejb.error = new CargaUYException(CodigoError.CASO_FUNDAMENTO_REQUERIDO);

        MockHttpResponse respuesta = api.post("/casos/3/resolucion", "{\"resultado\":\"DESESTIMADA\"}");

        assertEquals(400, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("\"codigo\":9003"));
    }

    @Test
    void resolverConPlazoVigenteDevuelve409() throws Exception {
        ejb.error = new CargaUYException(CodigoError.CASO_PLAZO_NO_VENCIDO);

        MockHttpResponse respuesta = api.post("/casos/4/resolucion", "{\"resultado\":\"CONFIRMADA\",\"fundamento\":\"x\"}");

        assertEquals(409, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("\"codigo\":9004"));
    }
}
