package tse.cargauy.web.ws.publico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.HabilitacionEmpresaDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.publico.ConsultaPublicaEJBLocal;
import tse.cargauy.web.ws.ApiEnMemoria;

class ConsultaPublicaRestTest {

    private Integer ultimoNro;
    private String ultimaRazonSocial;
    private CargaUYException error;
    private ApiEnMemoria api;

    @BeforeEach
    void preparar() {
        ConsultaPublicaRest rest = new ConsultaPublicaRest();
        rest.consultaPublicaEJB = new ConsultaPublicaEJBLocal() {
            @Override
            public HabilitacionEmpresaDto validarEmpresa(Integer nroEmpresa, String razonSocial) {
                ultimoNro = nroEmpresa;
                ultimaRazonSocial = razonSocial;
                if (error != null) {
                    throw error;
                }
                return new HabilitacionEmpresaDto("Transportes del Sur", "Transportes del Sur S.A.", true,
                        LocalDate.of(2026, 11, 14));
            }
        };
        api = new ApiEnMemoria(rest);
    }

    @Test
    void porNumero() throws Exception {
        MockHttpResponse respuesta = api.get("/publico/empresas/habilitacion?nro=1001");

        assertEquals(200, respuesta.getStatus());
        assertEquals(1001, ultimoNro);
        assertNull(ultimaRazonSocial);
        String json = respuesta.getContentAsString();
        assertTrue(json.contains("\"habilitada\":true"));
        assertTrue(json.contains("\"nombrePublico\":\"Transportes del Sur\""));
    }

    @Test
    void porRazonSocial() throws Exception {
        MockHttpResponse respuesta = api.get("/publico/empresas/habilitacion?razonSocial=Transportes%20del%20Sur%20S.A.");

        assertEquals(200, respuesta.getStatus());
        assertNull(ultimoNro);
        assertEquals("Transportes del Sur S.A.", ultimaRazonSocial);
    }

    @Test
    void soloDevuelveLaHabilitacion() throws Exception {
        String json = api.get("/publico/empresas/habilitacion?nro=1001").getContentAsString();

        assertFalse(json.contains("\"id\""));
        assertFalse(json.contains("direccion"));
        assertFalse(json.contains("nroEmpresa"));
    }

    @Test
    void sinResultado() throws Exception {
        error = new CargaUYException(CodigoError.EMPRESA_CONSULTA_SIN_RESULTADO);

        MockHttpResponse respuesta = api.get("/publico/empresas/habilitacion?nro=9999");

        assertEquals(404, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("1009"));
    }
}
