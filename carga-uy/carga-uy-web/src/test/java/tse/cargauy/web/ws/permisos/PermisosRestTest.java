package tse.cargauy.web.ws.permisos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.AprobacionPermisoDto;
import tse.cargauy.dtos.FiltroPermisos;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.RechazoPermisoDto;
import tse.cargauy.dtos.SolicitudPermisoDto;
import tse.cargauy.entities.EstadoPermiso;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.permiso.PermisoEJBLocal;
import tse.cargauy.web.ws.ApiEnMemoria;
import tse.cargauy.web.ws.EjbFalso;

class PermisosRestTest {

    private final EjbFalso<PermisoEJBLocal> ejb = EjbFalso.de(PermisoEJBLocal.class);
    private ApiEnMemoria api;

    @BeforeEach
    void preparar() {
        PermisosRest rest = new PermisosRest();
        rest.permisoEJB = ejb.ejb();
        api = new ApiEnMemoria(rest);
    }

    @Test
    void listaConLosFiltros() throws Exception {
        ejb.responde("listar", new PaginaDto<>(List.of(), 0, 0, 20));

        assertEquals(200, api.get("/permisos?idEmpresa=1&idVehiculo=10&estado=PENDIENTE").getStatus());

        FiltroPermisos filtro = (FiltroPermisos) ejb.argumento("listar", 0);
        assertEquals(1L, filtro.getIdEmpresa());
        assertEquals(10L, filtro.getIdVehiculo());
        assertEquals(EstadoPermiso.PENDIENTE, filtro.getEstado());
    }

    // el responsable solo declara numero, inicio y vehiculo: la fecha de fin y el estado no llegan al negocio (DA11)
    @Test
    void elAltaSoloLlevaLoQueDeclaraElResponsable() throws Exception {
        MockHttpResponse respuesta = api.post("/permisos",
                "{\"nroPermiso\":\"PNC-1\",\"validoDesde\":\"2026-11-01\",\"idVehiculo\":10,"
                        + "\"validoHasta\":\"2030-01-01\",\"estado\":\"APROBADO\"}");

        assertEquals(204, respuesta.getStatus());
        SolicitudPermisoDto solicitud = (SolicitudPermisoDto) ejb.argumento("addPermiso", 0);
        assertEquals("PNC-1", solicitud.getNroPermiso());
        assertEquals(LocalDate.of(2026, 11, 1), solicitud.getValidoDesde());
        assertEquals(10L, solicitud.getIdVehiculo());
    }

    @Test
    void consultarCorregirYBorrar() throws Exception {
        ejb.responde("getPermisoById", new PermisoDto());

        assertEquals(200, api.get("/permisos/4").getStatus());
        assertEquals(204, api.put("/permisos/4", "{\"nroPermiso\":\"PNC-2\"}").getStatus());
        assertEquals(204, api.delete("/permisos/4").getStatus());

        assertEquals(List.of("getPermisoById", "updatePermiso", "deletePermiso"), ejb.llamadas());
        assertEquals("PNC-2", ((SolicitudPermisoDto) ejb.argumento("updatePermiso", 1)).getNroPermiso());
    }

    @Test
    void elFuncionarioApruebaORechaza() throws Exception {
        assertEquals(204, api.post("/permisos/4/aprobacion", "{\"validoHasta\":\"2027-11-01\"}").getStatus());
        assertEquals(204, api.post("/permisos/5/rechazo", "{\"motivo\":\"no coincide\"}").getStatus());

        assertEquals(4L, ejb.argumento("aprobar", 0));
        assertEquals(LocalDate.of(2027, 11, 1), ((AprobacionPermisoDto) ejb.argumento("aprobar", 1)).getValidoHasta());
        assertEquals(5L, ejb.argumento("rechazar", 0));
        assertEquals("no coincide", ((RechazoPermisoDto) ejb.argumento("rechazar", 1)).getMotivo());
    }

    @Test
    void revisarUnoQueNoEstaPendienteEsConflicto() throws Exception {
        ejb.falla("aprobar", new CargaUYException(CodigoError.PERMISO_NO_PENDIENTE, "PNC-1"));

        MockHttpResponse respuesta = api.post("/permisos/4/aprobacion", "{}");

        assertEquals(409, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("3010"));
    }
}
