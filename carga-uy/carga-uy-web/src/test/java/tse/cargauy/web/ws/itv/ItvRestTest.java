package tse.cargauy.web.ws.itv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.ItvDto;
import tse.cargauy.negocio.itv.ItvEJBLocal;
import tse.cargauy.web.ws.ApiEnMemoria;
import tse.cargauy.web.ws.EjbFalso;

class ItvRestTest {

    @Test
    void itvDeUnVehiculo() throws Exception {
        EjbFalso<ItvEJBLocal> ejb = EjbFalso.de(ItvEJBLocal.class).responde("listar", List.of(new ItvDto()));
        ItvRest rest = new ItvRest();
        rest.itvEJB = ejb.ejb();
        ApiEnMemoria api = new ApiEnMemoria(rest);

        assertEquals(200, api.get("/vehiculos/10/itv").getStatus());
        assertEquals(204, api.post("/vehiculos/10/itv", "{}").getStatus());
        assertEquals(204, api.delete("/vehiculos/10/itv/2").getStatus());

        assertEquals(List.of("listar", "agregar", "eliminar"), ejb.llamadas());
        assertEquals(10L, ejb.argumento("agregar", 0));
        assertEquals(List.of(10L, 2L), List.of(ejb.argumentos("eliminar")));
    }
}
