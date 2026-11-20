package tse.cargauy.web.ws.choferes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.ChoferDto;
import tse.cargauy.dtos.FiltroChoferes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.negocio.chofer.ChoferEJBLocal;
import tse.cargauy.web.ws.ApiEnMemoria;
import tse.cargauy.web.ws.EjbFalso;

class ChoferesRestTest {

    private final EjbFalso<ChoferEJBLocal> ejb = EjbFalso.de(ChoferEJBLocal.class);
    private ApiEnMemoria api;

    @BeforeEach
    void preparar() {
        ChoferesRest rest = new ChoferesRest();
        rest.choferEJB = ejb.ejb();
        api = new ApiEnMemoria(rest);
        ejb.responde("getChoferes", new PaginaDto<>(List.of(), 0, 0, 20));
    }

    @Test
    void listaConLosFiltros() throws Exception {
        assertEquals(200, api.get("/empresas/7/choferes?cedula=11111111&fecha=2026-11-01&historial=true").getStatus());

        assertEquals(7L, ejb.argumento("getChoferes", 0));
        FiltroChoferes filtro = (FiltroChoferes) ejb.argumento("getChoferes", 1);
        assertEquals("11111111", filtro.getCedula());
        assertEquals(LocalDate.of(2026, 11, 1), filtro.getFecha());
        assertTrue(filtro.isHistorial());
    }

    @Test
    void sinFiltrosNoIncluyeElHistorial() throws Exception {
        api.get("/empresas/7/choferes");

        FiltroChoferes filtro = (FiltroChoferes) ejb.argumento("getChoferes", 1);
        assertNull(filtro.getCedula());
        assertFalse(filtro.isHistorial());
    }

    @Test
    void altaDevuelve201YBajaDevuelveElVinculo() throws Exception {
        ejb.responde("addChofer", args -> args[1]).responde("darDeBaja", new ChoferDto());

        MockHttpResponse alta = api.post("/empresas/7/choferes", "{}");
        MockHttpResponse baja = api.post("/empresas/7/choferes/9/baja", "");

        assertEquals(201, alta.getStatus());
        assertEquals(200, baja.getStatus());
        assertEquals(List.of(7L, 9L), List.of(ejb.argumentos("darDeBaja")));
    }
}
