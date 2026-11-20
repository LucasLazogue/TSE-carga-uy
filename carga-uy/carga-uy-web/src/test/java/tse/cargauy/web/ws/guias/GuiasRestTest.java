package tse.cargauy.web.ws.guias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.FiltroGuias;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.guia.GuiaEJBLocal;
import tse.cargauy.web.ws.ApiEnMemoria;
import tse.cargauy.web.ws.EjbFalso;

class GuiasRestTest {

    private final EjbFalso<GuiaEJBLocal> ejb = EjbFalso.de(GuiaEJBLocal.class);
    private ApiEnMemoria api;

    @BeforeEach
    void preparar() {
        GuiasRest rest = new GuiasRest();
        rest.guiaEJB = ejb.ejb();
        api = new ApiEnMemoria(rest);
        ejb.responde("listar", new PaginaDto<>(List.of(), 0, 0, 20));
    }

    @Test
    void listaLasGuiasDeLaEmpresaConLosFiltros() throws Exception {
        MockHttpResponse respuesta = api.get("/empresas/7/guias?busqueda=G-1&conViaje=true&desde=2026-11-01&hasta=2026-11-30&pagina=2&tamanio=5");

        assertEquals(200, respuesta.getStatus());
        assertEquals(7L, ejb.argumento("listar", 0));
        FiltroGuias filtro = (FiltroGuias) ejb.argumento("listar", 1);
        assertEquals("G-1", filtro.getBusqueda());
        assertEquals(Boolean.TRUE, filtro.getConViaje());
        assertEquals(LocalDate.of(2026, 11, 1), filtro.getDesde());
        assertEquals(LocalDate.of(2026, 11, 30), filtro.getHasta());
        Paginacion paginacion = (Paginacion) ejb.argumento("listar", 2);
        assertEquals(2, paginacion.getPagina());
        assertEquals(5, paginacion.getTamanio());
    }

    // sin filtros: todo vacio y la paginacion por defecto; una fecha vacia no es un error
    @Test
    void sinFiltrosUsaLosValoresPorDefecto() throws Exception {
        api.get("/empresas/7/guias?desde=");

        FiltroGuias filtro = (FiltroGuias) ejb.argumento("listar", 1);
        assertNull(filtro.getBusqueda());
        assertNull(filtro.getConViaje());
        assertNull(filtro.getDesde());
        Paginacion paginacion = (Paginacion) ejb.argumento("listar", 2);
        assertEquals(0, paginacion.getPagina());
        assertEquals(20, paginacion.getTamanio());
    }

    @Test
    void crearDevuelve201ConLaGuia() throws Exception {
        ejb.responde("addGuia", args -> {
            GuiaDto guia = (GuiaDto) args[1];
            guia.setId(3L);
            return guia;
        });

        MockHttpResponse respuesta = api.post("/empresas/7/guias", "{\"volumen\":1000,\"fecha\":\"2026-11-01\"}");

        assertEquals(201, respuesta.getStatus());
        GuiaDto recibida = (GuiaDto) ejb.argumento("addGuia", 1);
        assertEquals(1000, recibida.getVolumen());
        assertEquals(LocalDate.of(2026, 11, 1), recibida.getFecha());
        assertTrue(respuesta.getContentAsString().contains("\"id\":3"));
    }

    @Test
    void getModificarYBorrarUnaGuia() throws Exception {
        GuiaDto guia = new GuiaDto();
        guia.setNroGuia("G-3");
        ejb.responde("getGuia", guia).responde("updateGuia", guia);

        MockHttpResponse obtener = api.get("/empresas/7/guias/3");
        MockHttpResponse modificar = api.put("/empresas/7/guias/3", "{\"volumen\":500}");
        MockHttpResponse borrar = api.delete("/empresas/7/guias/3");

        assertEquals(200, obtener.getStatus());
        assertTrue(obtener.getContentAsString().contains("G-3"));
        assertEquals(200, modificar.getStatus());
        assertEquals(500, ((GuiaDto) ejb.argumento("updateGuia", 2)).getVolumen());
        assertEquals(204, borrar.getStatus());
        assertEquals(List.of(7L, 3L), List.of(ejb.argumentos("deleteGuia")));
    }

    // los errores del negocio llegan con su codigo HTTP y el codigo de error en el cuerpo
    @Test
    void losErroresDelNegocioSeTraducenAHttp() throws Exception {
        ejb.falla("getGuia", new CargaUYException(CodigoError.GUIA_NO_ENCONTRADA, "3"))
                .falla("addGuia", new CargaUYException(CodigoError.GUIA_FECHA_REQUERIDA))
                .falla("deleteGuia", new CargaUYException(CodigoError.ACCESO_DENEGADO));

        MockHttpResponse noEncontrada = api.get("/empresas/7/guias/3");
        MockHttpResponse invalida = api.post("/empresas/7/guias", "{}");
        MockHttpResponse prohibida = api.delete("/empresas/8/guias/3");

        assertEquals(404, noEncontrada.getStatus());
        assertTrue(noEncontrada.getContentAsString().contains("5011"));
        assertEquals(400, invalida.getStatus());
        assertEquals(403, prohibida.getStatus());
    }
}
