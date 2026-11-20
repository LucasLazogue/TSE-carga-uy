package tse.cargauy.web.ws.catalogos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.negocio.guia.GuiaEJBLocal;
import tse.cargauy.web.ws.ApiEnMemoria;
import tse.cargauy.web.ws.EjbFalso;

class CatalogosRestTest {

    @Test
    void rubrosYTiposDeCarga() throws Exception {
        EjbFalso<GuiaEJBLocal> ejb = EjbFalso.de(GuiaEJBLocal.class)
                .responde("getRubros", List.of(new RubroDto("Granos")))
                .responde("getTiposCarga", List.of(new TipoCargaDto()));
        CatalogosRest rest = new CatalogosRest();
        rest.guiaEJB = ejb.ejb();
        ApiEnMemoria api = new ApiEnMemoria(rest);

        MockHttpResponse rubros = api.get("/rubros");
        MockHttpResponse tipos = api.get("/tipos-carga");

        assertEquals(200, rubros.getStatus());
        assertTrue(rubros.getContentAsString().contains("Granos"));
        assertEquals(200, tipos.getStatus());
        assertEquals(List.of("getRubros", "getTiposCarga"), ejb.llamadas());
    }
}
