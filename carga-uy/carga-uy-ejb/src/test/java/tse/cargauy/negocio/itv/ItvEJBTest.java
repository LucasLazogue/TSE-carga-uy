package tse.cargauy.negocio.itv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.data.itv.ItvDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJB;

class ItvEJBTest {

    private static final LocalDate INSPECCION = LocalDate.of(2026, 1, 15);
    private static final LocalDate VENCIMIENTO = LocalDate.of(2027, 1, 15);
    private static final Long VEHICULO_DE_OTRA_EMPRESA = 20L;

    private final Map<Long, ItvDto> itvs = new HashMap<>();
    private final ItvEJB ejb = new ItvEJB();

    @BeforeEach
    void iniciar() {
        ejb.itvDAO = new ItvDAO() {
            @Override
            public ItvDto getItvById(Long id) {
                return itvs.get(id);
            }

            @Override
            public void addItv(ItvDto itvDto) {
                itvDto.setId((long) itvs.size() + 1);
                itvs.put(itvDto.getId(), itvDto);
            }

            @Override
            public void deleteItv(Long id) {
                itvs.remove(id);
            }
        };
        ejb.vehiculoDAO = new VehiculoDAO() {
            @Override
            public VehiculoDto getVehiculoById(Long id) {
                VehiculoDto vehiculo = new VehiculoDto();
                vehiculo.setId(id);
                vehiculo.setIdEmpresa(id.equals(VEHICULO_DE_OTRA_EMPRESA) ? 2L : 1L);
                return vehiculo;
            }
        };
        ejb.empresaEJB = new EmpresaEJB() {
            @Override
            public void validarAcceso(Long idEmpresa) {
                if (!idEmpresa.equals(1L)) {
                    throw new CargaUYException(CodigoError.ACCESO_DENEGADO);
                }
            }
        };
    }

    private static void assertCodigo(CodigoError esperado, Executable accion) {
        assertEquals(esperado, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    @Test
    void seAgregaUnaItvAlVehiculo() {
        ejb.agregar(10L, new ItvDto(null, INSPECCION, VENCIMIENTO));

        ItvDto creada = itvs.get(1L);
        assertEquals(10L, creada.getIdVehiculo());
        assertEquals(INSPECCION, creada.getFechaInspeccion());
        assertEquals(VENCIMIENTO, creada.getFechaVencimiento());
    }

    @Test
    void lasDosFechasSonObligatorias() {
        assertCodigo(CodigoError.ITV_FECHAS_REQUERIDAS, () -> ejb.agregar(10L, new ItvDto(null, null, VENCIMIENTO)));
        assertCodigo(CodigoError.ITV_FECHAS_REQUERIDAS, () -> ejb.agregar(10L, new ItvDto(null, INSPECCION, null)));
        assertTrue(itvs.isEmpty());
    }

    @Test
    void elVencimientoNoPuedeSerAnteriorALaInspeccion() {
        assertCodigo(CodigoError.ITV_RANGO_FECHAS_INVALIDO,
                () -> ejb.agregar(10L, new ItvDto(null, INSPECCION, INSPECCION.minusDays(1))));
        assertTrue(itvs.isEmpty());
    }

    @Test
    void noSeAgregaUnaItvAUnVehiculoDeOtraEmpresa() {
        assertCodigo(CodigoError.ACCESO_DENEGADO, () -> ejb.agregar(VEHICULO_DE_OTRA_EMPRESA, new ItvDto(null, INSPECCION, VENCIMIENTO)));
        assertTrue(itvs.isEmpty());
    }

    @Test
    void unaItvSoloSeEliminaDesdeSuVehiculo() {
        ejb.agregar(10L, new ItvDto(null, INSPECCION, VENCIMIENTO));

        assertCodigo(CodigoError.ITV_NO_ENCONTRADA, () -> ejb.eliminar(11L, 1L));
        ejb.eliminar(10L, 1L);
        assertTrue(itvs.isEmpty());
    }
}
