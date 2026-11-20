package tse.cargauy.web.ws.empresas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;
import tse.cargauy.web.ws.ApiEnMemoria;
import tse.cargauy.web.ws.EjbFalso;

class EmpresasRestTest {

    private final EjbFalso<EmpresaEJBLocal> ejb = EjbFalso.de(EmpresaEJBLocal.class);
    private ApiEnMemoria api;

    @BeforeEach
    void preparar() {
        EmpresasRest rest = new EmpresasRest();
        rest.empresaEJB = ejb.ejb();
        api = new ApiEnMemoria(rest);
        PaginaDto<EmpresaDto> vacia = new PaginaDto<>(List.of(), 0, 0, 20);
        ejb.responde("getAll", vacia).responde("findByNombre", vacia);
    }

    @Test
    void sinNombreListaTodas() throws Exception {
        assertEquals(200, api.get("/empresas?pagina=1").getStatus());
        assertEquals(200, api.get("/empresas?nombre=").getStatus());

        assertEquals(List.of("getAll", "getAll"), ejb.llamadas());
        assertEquals(0, ((Paginacion) ejb.argumento("getAll", 0)).getPagina(), "la ultima, sin pagina, va a la primera");
    }

    @Test
    void conNombreBusca() throws Exception {
        assertEquals(200, api.get("/empresas?nombre=Sur").getStatus());

        assertEquals("Sur", ejb.argumento("findByNombre", 0));
    }

    @Test
    void altaDevuelve201() throws Exception {
        ejb.responde("addEmpresa", args -> args[0]);

        MockHttpResponse respuesta = api.post("/empresas", "{\"nroEmpresa\":123,\"nombrePublico\":\"Transportes Sur\"}");

        assertEquals(201, respuesta.getStatus());
        EmpresaDto empresa = (EmpresaDto) ejb.argumento("addEmpresa", 0);
        assertEquals(123, empresa.getNroEmpresa());
        assertEquals("Transportes Sur", empresa.getNombrePublico());
    }

    @Test
    void consultarModificarYBorrar() throws Exception {
        ejb.responde("getEmpresaById", new EmpresaDto());

        assertEquals(200, api.get("/empresas/1").getStatus());
        assertEquals(204, api.put("/empresas/1", "{\"razonSocial\":\"Sur S.A.\"}").getStatus());
        assertEquals(204, api.delete("/empresas/1").getStatus());

        assertEquals("Sur S.A.", ((EmpresaDto) ejb.argumento("updateEmpresa", 1)).getRazonSocial());
        assertEquals(1L, ejb.argumento("deleteEmpresa", 0));
    }

    @Test
    void unaEmpresaInexistente() throws Exception {
        ejb.falla("getEmpresaById", new CargaUYException(CodigoError.EMPRESA_NO_ENCONTRADA, "9"));

        assertEquals(404, api.get("/empresas/9").getStatus());
    }
}
