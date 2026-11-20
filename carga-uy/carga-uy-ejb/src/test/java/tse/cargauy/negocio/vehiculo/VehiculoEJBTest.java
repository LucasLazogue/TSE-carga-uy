package tse.cargauy.negocio.vehiculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.data.empresa.EmpresaDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.FiltroVehiculos;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJB;

class VehiculoEJBTest {

    private static final Long PROPIA = 1L;
    private static final Long AJENA = 2L;
    private static final Paginacion PAGINA = new Paginacion(0, 20);

    private final Map<Long, VehiculoDto> vehiculos = new HashMap<>();
    private final VehiculoEJB ejb = new VehiculoEJB();
    private Integer capacidadPedida;

    @BeforeEach
    void iniciar() {
        ejb.vehiculoDAO = new VehiculoDAO() {
            @Override
            public VehiculoDto getVehiculoById(Long id) {
                return vehiculos.get(id);
            }

            @Override
            public VehiculoDto getVehiculoByMatricula(String matricula) {
                return vehiculos.values().stream().filter(v -> v.getMatricula().equals(matricula)).findFirst().orElse(null);
            }

            @Override
            public PaginaDto<VehiculoDto> getAll(Paginacion paginacion) {
                List<VehiculoDto> items = List.copyOf(vehiculos.values());
                return new PaginaDto<>(items, items.size(), paginacion.getPagina(), paginacion.getTamanio());
            }

            @Override
            public PaginaDto<VehiculoDto> getByEmpresa(FiltroVehiculos filtro, Paginacion paginacion) {
                capacidadPedida = filtro.getCapacidadMinima();
                List<VehiculoDto> items = vehiculos.values().stream()
                        .filter(v -> v.getIdEmpresa().equals(filtro.getIdEmpresa())).toList();
                return new PaginaDto<>(items, items.size(), paginacion.getPagina(), paginacion.getTamanio());
            }

            @Override
            public void addVehiculo(VehiculoDto vehiculoDto) {
                vehiculoDto.setId((long) vehiculos.size() + 1);
                vehiculos.put(vehiculoDto.getId(), vehiculoDto);
            }

            @Override
            public void updateVehiculo(Long id, VehiculoDto vehiculoDto) {
                vehiculoDto.setId(id);
                vehiculos.put(id, vehiculoDto);
            }

            @Override
            public void deleteVehiculo(Long id) {
                vehiculos.remove(id);
            }
        };
        ejb.empresaDAO = new EmpresaDAO() {
            @Override
            public EmpresaDto getEmpresaById(Long id) {
                return id.equals(PROPIA) || id.equals(AJENA) ? new EmpresaDto() : null;
            }
        };
        ejb.empresaEJB = new EmpresaEJB() {
            @Override
            public void validarAcceso(Long idEmpresa) {
                if (!PROPIA.equals(idEmpresa)) {
                    throw new CargaUYException(CodigoError.ACCESO_DENEGADO);
                }
            }
        };
        ejb.vehiculoDAO.addVehiculo(new VehiculoDto("STA1234", "Volvo", "FH 460", 8500, 24000, PROPIA));
        ejb.vehiculoDAO.addVehiculo(new VehiculoDto("SCC9012", "Mercedes-Benz", "Actros 2646", 8800, 25000, AJENA));
    }

    private static void assertCodigo(CodigoError esperado, Executable accion) {
        assertEquals(esperado, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    private static VehiculoDto scania(int peso, int capacidad, Long idEmpresa) {
        return new VehiculoDto("SBB5678", "Scania", "R 450", peso, capacidad, idEmpresa);
    }

    @Test
    void seAgregaUnVehiculoALaEmpresa() {
        ejb.addVehiculo(scania(9000, 26000, PROPIA));

        VehiculoDto creado = vehiculos.get(3L);
        assertEquals("SBB5678", creado.getMatricula());
        assertEquals(26000, creado.getCapacidadCarga());
        assertEquals(PROPIA, creado.getIdEmpresa());
    }

    @Test
    void losDatosDelVehiculoSeValidan() {
        assertCodigo(CodigoError.VEHICULO_MATRICULA_REQUERIDA,
                () -> ejb.addVehiculo(new VehiculoDto(null, "Scania", "R 450", 9000, 26000, PROPIA)));
        assertCodigo(CodigoError.VEHICULO_MATRICULA_REQUERIDA,
                () -> ejb.addVehiculo(new VehiculoDto("  ", "Scania", "R 450", 9000, 26000, PROPIA)));
        assertCodigo(CodigoError.VEHICULO_MARCA_REQUERIDA,
                () -> ejb.addVehiculo(new VehiculoDto("SBB5678", "", "R 450", 9000, 26000, PROPIA)));
        assertCodigo(CodigoError.VEHICULO_PESO_INVALIDO, () -> ejb.addVehiculo(scania(0, 26000, PROPIA)));
        assertCodigo(CodigoError.VEHICULO_CAPACIDAD_INVALIDA, () -> ejb.addVehiculo(scania(9000, 0, PROPIA)));
        assertCodigo(CodigoError.VEHICULO_EMPRESA_REQUERIDA, () -> ejb.addVehiculo(scania(9000, 26000, null)));
        assertCodigo(CodigoError.EMPRESA_NO_ENCONTRADA, () -> ejb.addVehiculo(scania(9000, 26000, 99L)));
        assertEquals(2, vehiculos.size());
    }

    @Test
    void laMatriculaNoSePuedeRepetir() {
        assertCodigo(CodigoError.VEHICULO_MATRICULA_DUPLICADA,
                () -> ejb.addVehiculo(new VehiculoDto("STA1234", "Scania", "R 450", 9000, 26000, PROPIA)));
        assertEquals(2, vehiculos.size());
    }

    @Test
    void alEditarSeConservaLaMatriculaPropiaPeroNoSeTomaLaDeOtro() {
        ejb.updateVehiculo(1L, new VehiculoDto("STA1234", "Volvo", "FH 500", 8500, 27000, PROPIA));
        assertEquals(27000, vehiculos.get(1L).getCapacidadCarga());

        assertCodigo(CodigoError.VEHICULO_MATRICULA_DUPLICADA,
                () -> ejb.updateVehiculo(1L, new VehiculoDto("SCC9012", "Volvo", "FH 500", 8500, 27000, PROPIA)));
        assertEquals("STA1234", vehiculos.get(1L).getMatricula());
    }

    @Test
    void noSeAccedeALosVehiculosDeOtraEmpresa() {
        assertCodigo(CodigoError.ACCESO_DENEGADO, () -> ejb.getVehiculoById(2L));
        assertCodigo(CodigoError.ACCESO_DENEGADO, () -> ejb.getByEmpresa(new FiltroVehiculos(AJENA, null), PAGINA));
        assertCodigo(CodigoError.ACCESO_DENEGADO, () -> ejb.addVehiculo(scania(9000, 26000, AJENA)));
        assertCodigo(CodigoError.ACCESO_DENEGADO,
                () -> ejb.updateVehiculo(2L, new VehiculoDto("SCC9012", "Mercedes-Benz", "Actros", 8800, 30000, AJENA)));
        assertCodigo(CodigoError.ACCESO_DENEGADO, () -> ejb.deleteVehiculo(2L));

        assertEquals(25000, vehiculos.get(2L).getCapacidadCarga());
    }

    @Test
    void unVehiculoNoSePuedePasarAOtraEmpresa() {
        assertCodigo(CodigoError.ACCESO_DENEGADO,
                () -> ejb.updateVehiculo(1L, new VehiculoDto("STA1234", "Volvo", "FH 460", 8500, 24000, AJENA)));
        assertEquals(PROPIA, vehiculos.get(1L).getIdEmpresa());
    }

    @Test
    void elListadoDeLaEmpresaLePasaLaCapacidadMinimaAlDao() {
        PaginaDto<VehiculoDto> pagina = ejb.getByEmpresa(new FiltroVehiculos(PROPIA, 25000), PAGINA);

        assertEquals(25000, capacidadPedida);
        assertEquals(List.of("STA1234"), pagina.getItems().stream().map(VehiculoDto::getMatricula).toList());
    }

    // el listado sin empresa es solo del funcionario (@RolesAllowed), que ve los de todas
    @Test
    void elListadoCompletoIncluyeTodasLasEmpresas() {
        assertEquals(2, ejb.getAll(PAGINA).getTotal());
    }

    @Test
    void seConsultaYSeBorraUnVehiculoDeLaEmpresa() {
        assertEquals("STA1234", ejb.getVehiculoById(1L).getMatricula());
        assertNull(ejb.getVehiculoById(99L));

        ejb.deleteVehiculo(1L);

        assertNull(vehiculos.get(1L));
    }
}
