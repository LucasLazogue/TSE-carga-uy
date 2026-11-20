package tse.cargauy.web.ws.viajes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PesajesViajeDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.viaje.ViajeEJBLocal;
import tse.cargauy.web.ws.ApiEnMemoria;
import tse.cargauy.web.ws.EjbFalso;

class ViajesRestTest {

    private final EjbFalso<ViajeEJBLocal> ejb = EjbFalso.de(ViajeEJBLocal.class);
    private ApiEnMemoria api;

    @BeforeEach
    void preparar() {
        ViajesRest rest = new ViajesRest();
        rest.viajeEJB = ejb.ejb();
        api = new ApiEnMemoria(rest);
        PaginaDto<Object> vacia = new PaginaDto<>(List.of(), 0, 0, 20);
        ejb.responde("listar", vacia).responde("getEventos", vacia);
    }

    @Test
    void listaConLosFiltrosDelChoferVehiculoYEstado() throws Exception {
        MockHttpResponse respuesta = api.get("/empresas/7/viajes?idChofer=5&idVehiculo=3&estado=EN_CURSO");

        assertEquals(200, respuesta.getStatus());
        FiltroViajes filtro = (FiltroViajes) ejb.argumento("listar", 1);
        assertEquals(5L, filtro.getIdChofer());
        assertEquals(3L, filtro.getIdVehiculo());
        assertEquals(EstadoViaje.EN_CURSO, filtro.getEstado());
    }

    @Test
    void sinFiltros() throws Exception {
        api.get("/empresas/7/viajes");

        FiltroViajes filtro = (FiltroViajes) ejb.argumento("listar", 1);
        assertNull(filtro.getIdChofer());
        assertNull(filtro.getEstado());
    }

    @Test
    void asignarDevuelve201() throws Exception {
        ejb.responde("asignarViaje", args -> args[1]);

        MockHttpResponse respuesta = api.post("/empresas/7/viajes", "{\"idGuia\":2,\"idVehiculo\":3,\"idChofer\":5}");

        assertEquals(201, respuesta.getStatus());
        ViajeDto recibido = (ViajeDto) ejb.argumento("asignarViaje", 1);
        assertEquals(2L, recibido.getIdGuia());
        assertEquals(3L, recibido.getIdVehiculo());
        assertEquals(5L, recibido.getIdChofer());
    }

    @Test
    void consultasDeUnViaje() throws Exception {
        ejb.responde("getViaje", new ViajeDto())
                .responde("getPesajes", new PesajesViajeDto())
                .responde("getPosiciones", List.of(new EventoPosicionDto()));

        assertEquals(200, api.get("/empresas/7/viajes/1").getStatus());
        assertEquals(200, api.get("/empresas/7/viajes/1/eventos?tamanio=50").getStatus());
        assertEquals(200, api.get("/empresas/7/viajes/1/pesajes").getStatus());
        assertEquals(200, api.get("/empresas/7/viajes/1/posiciones").getStatus());

        assertEquals(List.of("getViaje", "getEventos", "getPesajes", "getPosiciones"), ejb.llamadas());
        assertEquals(50, ((Paginacion) ejb.argumento("getEventos", 2)).getTamanio());
        assertEquals(1L, ejb.argumento("getPosiciones", 1));
    }

    @Test
    void reasignarYBorrar() throws Exception {
        ejb.responde("reasignarViaje", new ViajeDto());

        assertEquals(200, api.put("/empresas/7/viajes/1", "{\"idVehiculo\":4,\"idChofer\":6}").getStatus());
        assertEquals(204, api.delete("/empresas/7/viajes/1").getStatus());

        assertEquals(4L, ((ViajeDto) ejb.argumento("reasignarViaje", 2)).getIdVehiculo());
        assertEquals(1L, ejb.argumento("deleteViaje", 1));
    }

    @Test
    void unViajeEnCursoNoSeModifica() throws Exception {
        ejb.falla("deleteViaje", new CargaUYException(CodigoError.VIAJE_NO_MODIFICABLE, "1"));

        MockHttpResponse respuesta = api.delete("/empresas/7/viajes/1");

        assertEquals(409, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("6010"));
    }
}
