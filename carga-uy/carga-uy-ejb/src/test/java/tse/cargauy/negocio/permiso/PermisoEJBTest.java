package tse.cargauy.negocio.permiso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.data.permiso.PermisoDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.SolicitudPermisoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.EstadoPermiso;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJB;

class PermisoEJBTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);

    private final Map<Long, PermisoDto> permisos = new HashMap<>();
    private final PermisoEJB ejb = new PermisoEJB();

    @BeforeEach
    void iniciar() {
        ejb.permisoDAO = new PermisoDAO() {
            @Override
            public PermisoDto getPermisoById(Long id) {
                return permisos.get(id);
            }

            @Override
            public PermisoDto getPermisoByNro(String nroPermiso) {
                return permisos.values().stream().filter(p -> p.getNroPermiso().equals(nroPermiso)).findFirst()
                        .orElse(null);
            }

            @Override
            public void addPermiso(PermisoDto permisoDto) {
                permisoDto.setId((long) permisos.size() + 1);
                permisos.put(permisoDto.getId(), permisoDto);
            }

            @Override
            public void updatePermiso(Long id, PermisoDto permisoDto) {
                permisoDto.setId(id);
                permisos.put(id, permisoDto);
            }

            @Override
            public void deletePermiso(Long id) {
                permisos.remove(id);
            }
        };
        ejb.vehiculoDAO = new VehiculoDAO() {
            @Override
            public VehiculoDto getVehiculoById(Long id) {
                VehiculoDto vehiculo = new VehiculoDto();
                vehiculo.setId(id);
                vehiculo.setIdEmpresa(1L);
                return vehiculo;
            }
        };
        ejb.empresaEJB = new EmpresaEJB() {
            @Override
            public void validarAcceso(Long idEmpresa) {
            }
        };
    }

    private static void assertCodigo(CodigoError esperado, Executable accion) {
        assertEquals(esperado, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    private void aprobado(Long id) {
        PermisoDto permiso = new PermisoDto("PNC-1", DESDE, DESDE.plusYears(1), 10L, EstadoPermiso.APROBADO);
        permiso.setId(id);
        permisos.put(id, permiso);
    }

    @Test
    void elAltaQuedaPendienteYSinFechaDeFin() {
        ejb.addPermiso(new SolicitudPermisoDto("PNC-2", DESDE, 10L));

        PermisoDto creado = permisos.get(1L);
        assertEquals(EstadoPermiso.PENDIENTE, creado.getEstado());
        assertEquals(DESDE, creado.getValidoDesde());
        assertNull(creado.getValidoHasta());
    }

    @Test
    void unPendienteSeCorrigeYSigueSinFechaDeFin() {
        ejb.addPermiso(new SolicitudPermisoDto("PNC-2", DESDE, 10L));

        ejb.updatePermiso(1L, new SolicitudPermisoDto("PNC-3", DESDE.plusDays(1), 10L));

        PermisoDto corregido = permisos.get(1L);
        assertEquals("PNC-3", corregido.getNroPermiso());
        assertEquals(EstadoPermiso.PENDIENTE, corregido.getEstado());
        assertNull(corregido.getValidoHasta());
    }

    @Test
    void unAprobadoNoSeModificaNiSeElimina() {
        aprobado(1L);

        assertCodigo(CodigoError.PERMISO_NO_MODIFICABLE,
                () -> ejb.updatePermiso(1L, new SolicitudPermisoDto("PNC-1", DESDE, 10L)));
        assertCodigo(CodigoError.PERMISO_NO_MODIFICABLE, () -> ejb.deletePermiso(1L));
        assertEquals(EstadoPermiso.APROBADO, permisos.get(1L).getEstado());
    }

    @Test
    void elAltaPideFechaDeInicio() {
        assertCodigo(CodigoError.PERMISO_INICIO_REQUERIDO,
                () -> ejb.addPermiso(new SolicitudPermisoDto("PNC-2", null, 10L)));
        assertTrue(permisos.isEmpty());
    }
}
