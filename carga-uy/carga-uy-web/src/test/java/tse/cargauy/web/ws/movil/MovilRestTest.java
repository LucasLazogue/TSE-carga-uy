package tse.cargauy.web.ws.movil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.SyncRespuestaDto;
import tse.cargauy.dtos.ViajeMovilDto;
import tse.cargauy.negocio.movil.ViajeChoferEJBLocal;
import tse.cargauy.web.ws.ApiEnMemoria;
import tse.cargauy.web.ws.EjbFalso;

class MovilRestTest {

    @Test
    void elMovilListaYSincroniza() throws Exception {
        SyncRespuestaDto sync = new SyncRespuestaDto(List.of(), List.of());
        EjbFalso<ViajeChoferEJBLocal> ejb = EjbFalso.de(ViajeChoferEJBLocal.class)
                .responde("getViajes", List.of(new ViajeMovilDto()))
                .responde("sincronizar", sync);
        MovilRest rest = new MovilRest();
        rest.viajeChoferEJB = ejb.ejb();
        ApiEnMemoria api = new ApiEnMemoria(rest);

        assertEquals(200, api.get("/movil/viajes").getStatus());
        MockHttpResponse respuesta = api.post("/movil/sync", "[{\"idEvento\":\"e-1\"},{\"idEvento\":\"e-2\"}]");

        assertEquals(200, respuesta.getStatus());
        @SuppressWarnings("unchecked")
        List<EventoViajeDto> eventos = (List<EventoViajeDto>) ejb.argumento("sincronizar", 0);
        assertEquals(2, eventos.size());
    }
}
