package tse.cargauy.web.ws.vehiculos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.FiltroVehiculos;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.vehiculo.VehiculoEJB;
import tse.cargauy.web.ws.ApiEnMemoria;

class VehiculosRestTest {

    private String metodo;
    private Long empresaPedida;
    private Integer capacidadPedida;
    private Paginacion paginacionPedida;
    private VehiculoDto creado;
    private ApiEnMemoria api;

    @BeforeEach
    void preparar() {
        VehiculosRest rest = new VehiculosRest();
        rest.vehiculoEJB = new VehiculoEJB() {
            @Override
            public PaginaDto<VehiculoDto> getAll(Paginacion paginacion) {
                metodo = "getAll";
                paginacionPedida = paginacion;
                return new PaginaDto<>(List.of(), 0, paginacion.getPagina(), paginacion.getTamanio());
            }

            @Override
            public PaginaDto<VehiculoDto> getByEmpresa(FiltroVehiculos filtro, Paginacion paginacion) {
                if (filtro.getIdEmpresa() == 2L) {
                    throw new CargaUYException(CodigoError.ACCESO_DENEGADO);
                }
                metodo = "getByEmpresa";
                empresaPedida = filtro.getIdEmpresa();
                capacidadPedida = filtro.getCapacidadMinima();
                paginacionPedida = paginacion;
                VehiculoDto scania = new VehiculoDto("SBB5678", "Scania", "R 450", 9000, 26000, filtro.getIdEmpresa());
                return new PaginaDto<>(List.of(scania), 1, paginacion.getPagina(), paginacion.getTamanio());
            }

            @Override
            public VehiculoDto getVehiculoById(Long id) {
                VehiculoDto volvo = new VehiculoDto("STA1234", "Volvo", "FH 460", 8500, 24000, 1L);
                volvo.setId(id);
                return volvo;
            }

            @Override
            public void addVehiculo(VehiculoDto vehiculoDto) {
                creado = vehiculoDto;
            }
        };
        api = new ApiEnMemoria(rest);
    }

    @Test
    void sinEmpresaSeListanTodosConLaPaginacionPorDefecto() throws Exception {
        MockHttpResponse respuesta = api.get("/vehiculos");

        assertEquals(200, respuesta.getStatus());
        assertEquals("getAll", metodo);
        assertEquals(0, paginacionPedida.getPagina());
        assertEquals(20, paginacionPedida.getTamanio());
    }

    @Test
    void laCapacidadMinimaYLaPaginacionLleganAlEjb() throws Exception {
        MockHttpResponse respuesta = api.get("/vehiculos?idEmpresa=1&capacidadMinima=25000&pagina=1&tamanio=5");

        assertEquals(200, respuesta.getStatus());
        assertEquals("getByEmpresa", metodo);
        assertEquals(1L, empresaPedida);
        assertEquals(25000, capacidadPedida);
        assertEquals(1, paginacionPedida.getPagina());
        assertEquals(5, paginacionPedida.getTamanio());
        assertTrue(respuesta.getContentAsString().contains("\"capacidadCarga\":26000"));
    }

    @Test
    void sinCapacidadMinimaNoSeFiltra() throws Exception {
        api.get("/vehiculos?idEmpresa=1");

        assertEquals("getByEmpresa", metodo);
        assertNull(capacidadPedida);
    }

    @Test
    void losVehiculosDeOtraEmpresaDanProhibido() throws Exception {
        MockHttpResponse respuesta = api.get("/vehiculos?idEmpresa=2&capacidadMinima=1");

        assertEquals(403, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("\"codigo\":" + CodigoError.ACCESO_DENEGADO.getCodigo()));
    }

    @Test
    void seConsultaUnVehiculoPorId() throws Exception {
        MockHttpResponse respuesta = api.get("/vehiculos/7");

        assertEquals(200, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("\"id\":7"));
        assertTrue(respuesta.getContentAsString().contains("\"matricula\":\"STA1234\""));
    }

    @Test
    void seCreaUnVehiculo() throws Exception {
        MockHttpResponse respuesta = api.post("/vehiculos",
                "{\"matricula\":\"SBB5678\",\"marca\":\"Scania\",\"modelo\":\"R 450\",\"pesoVehiculo\":9000,"
                        + "\"capacidadCarga\":26000,\"idEmpresa\":1}");

        assertEquals(204, respuesta.getStatus());
        assertEquals("SBB5678", creado.getMatricula());
        assertEquals(26000, creado.getCapacidadCarga());
        assertEquals(1L, creado.getIdEmpresa());
    }
}
