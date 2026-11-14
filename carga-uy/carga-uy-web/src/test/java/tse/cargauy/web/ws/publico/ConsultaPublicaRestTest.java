package tse.cargauy.web.ws.publico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.HabilitacionEmpresaDto;
import tse.cargauy.dtos.IndicadoresPublicosDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.ViajesPorMesPublicoDto;
import tse.cargauy.dtos.VolumenPorRubroPublicoDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.publico.ConsultaPublicaEJBLocal;
import tse.cargauy.web.ws.ApiEnMemoria;

class ConsultaPublicaRestTest {

    private Integer ultimoNro;
    private String ultimaRazonSocial;
    private FiltroInforme ultimoFiltro;
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

            @Override
            public IndicadoresPublicosDto getIndicadores(FiltroInforme filtro) {
                ultimoFiltro = filtro;
                if (error != null) {
                    throw error;
                }
                IndicadoresPublicosDto indicadores = new IndicadoresPublicosDto();
                indicadores.setUmbral(5);
                indicadores.setViajesPorMes(List.of(new ViajesPorMesPublicoDto("2026-03", 18L), new ViajesPorMesPublicoDto("2026-04", null)));
                indicadores.setVolumenPorRubro(List.of(new VolumenPorRubroPublicoDto("Forestal", 48000L),
                        new VolumenPorRubroPublicoDto("Granos", null)));
                return indicadores;
            }

            @Override
            public List<RubroDto> getRubros() {
                return List.of(new RubroDto("Forestal"));
            }

            @Override
            public List<TipoCargaDto> getTiposCarga() {
                return List.of(new TipoCargaDto("Granos"));
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

    @Test
    void indicadoresConFiltros() throws Exception {
        MockHttpResponse respuesta = api.get("/publico/indicadores?desde=2026-03-01&hasta=2026-06-30&tipoCarga=2&rubro=4");

        assertEquals(200, respuesta.getStatus());
        assertEquals(LocalDate.of(2026, 3, 1), ultimoFiltro.getDesde());
        assertEquals(LocalDate.of(2026, 6, 30), ultimoFiltro.getHasta());
        assertEquals(2L, ultimoFiltro.getIdTipoCarga());
        assertEquals(4L, ultimoFiltro.getIdRubro());
    }

    @Test
    void elMesBajoElUmbralNoTraeCantidad() throws Exception {
        String json = api.get("/publico/indicadores?desde=2026-03-01&hasta=2026-06-30").getContentAsString();

        assertTrue(json.contains("{\"cantidad\":18,\"mes\":\"2026-03\"}"));
        assertTrue(json.contains("{\"mes\":\"2026-04\"}"));
    }

    @Test
    void elRubroBajoElUmbralNoTraeVolumen() throws Exception {
        String json = api.get("/publico/indicadores?desde=2026-03-01&hasta=2026-06-30").getContentAsString();

        assertTrue(json.contains("{\"rubro\":\"Forestal\",\"volumen\":48000}"));
        assertTrue(json.contains("{\"rubro\":\"Granos\"}"));
    }

    @Test
    void catalogosSinSesion() throws Exception {
        MockHttpResponse rubros = api.get("/publico/rubros");
        MockHttpResponse tipos = api.get("/publico/tipos-carga");

        assertEquals(200, rubros.getStatus());
        assertTrue(rubros.getContentAsString().contains("\"nombre\":\"Forestal\""));
        assertEquals(200, tipos.getStatus());
        assertTrue(tipos.getContentAsString().contains("\"nombre\":\"Granos\""));
    }

    @Test
    void periodoInvalido() throws Exception {
        error = new CargaUYException(CodigoError.INFORME_RANGO_FECHAS_INVALIDO);

        MockHttpResponse respuesta = api.get("/publico/indicadores?desde=2026-03-31&hasta=2026-03-01");

        assertEquals(400, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("12002"));
    }
}
