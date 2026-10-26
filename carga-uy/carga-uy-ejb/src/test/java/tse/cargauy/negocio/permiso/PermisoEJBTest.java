package tse.cargauy.negocio.permiso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.data.permiso.PermisoDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import jakarta.ejb.SessionContext;
import tse.cargauy.dtos.AprobacionPermisoDto;
import tse.cargauy.dtos.FiltroPermisos;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.RechazoPermisoDto;
import tse.cargauy.dtos.SolicitudPermisoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.EstadoPermiso;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJB;

class PermisoEJBTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 25, 10, 0);

    private final Map<Long, PermisoDto> permisos = new HashMap<>();
    private final PermisoEJB ejb = new PermisoEJB() {
        @Override
        Long idUsuario() {
            return 9L;
        }

        @Override
        LocalDateTime ahora() {
            return AHORA;
        }
    };

    @BeforeEach
    void iniciar() {
        ejb.permisoDAO = new PermisoDAO() {
            @Override
            public PermisoDto getPermisoById(Long id) {
                return permisos.get(id);
            }

            @Override
            public PermisoDto getSuperpuesto(Long idVehiculo, LocalDate desde, LocalDate hasta, Long idExcluir) {
                return permisos.values().stream()
                        .filter(p -> !p.getId().equals(idExcluir) && p.getValidoHasta() != null)
                        .filter(p -> !p.getValidoDesde().isAfter(hasta) && !p.getValidoHasta().isBefore(desde))
                        .findFirst().orElse(null);
            }

            @Override
            public void aprobar(Long id, LocalDate desde, LocalDate hasta, Long idFuncionario, LocalDateTime fecha) {
                PermisoDto permiso = permisos.get(id);
                permiso.setEstado(EstadoPermiso.APROBADO);
                permiso.setValidoDesde(desde);
                permiso.setValidoHasta(hasta);
                permiso.setCedulaRevisor(String.valueOf(idFuncionario));
                permiso.setFechaRevision(fecha);
            }

            @Override
            public void rechazar(Long id, String motivo, Long idFuncionario, LocalDateTime fecha) {
                PermisoDto permiso = permisos.get(id);
                permiso.setEstado(EstadoPermiso.RECHAZADO);
                permiso.setMotivoRechazo(motivo);
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
    void elFuncionarioApruebaConLaFechaDeFin() {
        ejb.addPermiso(new SolicitudPermisoDto("PNC-2", DESDE, 10L));

        ejb.aprobar(1L, new AprobacionPermisoDto(null, DESDE.plusYears(1)));

        PermisoDto aprobado = permisos.get(1L);
        assertEquals(EstadoPermiso.APROBADO, aprobado.getEstado());
        assertEquals(DESDE, aprobado.getValidoDesde());
        assertEquals(DESDE.plusYears(1), aprobado.getValidoHasta());
        assertEquals("9", aprobado.getCedulaRevisor());
        assertEquals(AHORA, aprobado.getFechaRevision());
    }

    @Test
    void alAprobarPuedeCorregirLaFechaDeInicio() {
        ejb.addPermiso(new SolicitudPermisoDto("PNC-2", DESDE, 10L));

        ejb.aprobar(1L, new AprobacionPermisoDto(DESDE.plusDays(5), DESDE.plusYears(1)));

        assertEquals(DESDE.plusDays(5), permisos.get(1L).getValidoDesde());
    }

    @Test
    void laFechaDeFinEsObligatoriaYValida() {
        ejb.addPermiso(new SolicitudPermisoDto("PNC-2", LocalDate.of(2026, 1, 1), 10L));

        assertCodigo(CodigoError.PERMISO_FIN_REQUERIDO, () -> ejb.aprobar(1L, new AprobacionPermisoDto(null, null)));
        assertCodigo(CodigoError.PERMISO_RANGO_FECHAS_INVALIDO,
                () -> ejb.aprobar(1L, new AprobacionPermisoDto(null, LocalDate.of(2025, 12, 31))));
        assertCodigo(CodigoError.PERMISO_VENCIDO,
                () -> ejb.aprobar(1L, new AprobacionPermisoDto(null, AHORA.toLocalDate().minusDays(1))));
        assertEquals(EstadoPermiso.PENDIENTE, permisos.get(1L).getEstado());
    }

    @Test
    void noSeApruebaSiSeSuperponeConOtroAprobado() {
        aprobado(1L);
        ejb.addPermiso(new SolicitudPermisoDto("PNC-2", DESDE.plusMonths(6), 10L));

        assertCodigo(CodigoError.PERMISO_SUPERPUESTO,
                () -> ejb.aprobar(2L, new AprobacionPermisoDto(null, DESDE.plusYears(2))));
    }

    @Test
    void soloSeRevisaUnPendiente() {
        aprobado(1L);

        assertCodigo(CodigoError.PERMISO_NO_PENDIENTE,
                () -> ejb.aprobar(1L, new AprobacionPermisoDto(null, DESDE.plusYears(2))));
        assertCodigo(CodigoError.PERMISO_NO_PENDIENTE, () -> ejb.rechazar(1L, new RechazoPermisoDto("motivo")));
        assertCodigo(CodigoError.PERMISO_NO_ENCONTRADO, () -> ejb.rechazar(5L, new RechazoPermisoDto("motivo")));
    }

    @Test
    void elRechazoPideMotivoYElResponsableLoReenvia() {
        ejb.addPermiso(new SolicitudPermisoDto("PNC-2", DESDE, 10L));

        assertCodigo(CodigoError.PERMISO_MOTIVO_REQUERIDO, () -> ejb.rechazar(1L, new RechazoPermisoDto(" ")));
        ejb.rechazar(1L, new RechazoPermisoDto(" El numero no coincide "));
        assertEquals(EstadoPermiso.RECHAZADO, permisos.get(1L).getEstado());
        assertEquals("El numero no coincide", permisos.get(1L).getMotivoRechazo());

        ejb.updatePermiso(1L, new SolicitudPermisoDto("PNC-3", DESDE, 10L));
        assertEquals(EstadoPermiso.PENDIENTE, permisos.get(1L).getEstado());
        assertNull(permisos.get(1L).getMotivoRechazo());
    }

    @Test
    void elResponsableNoListaPermisosSinIndicarElVehiculo() {
        ejb.contexto = (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { SessionContext.class },
                (proxy, metodo, args) -> metodo.getName().equals("isCallerInRole") ? Boolean.FALSE : null);

        assertCodigo(CodigoError.ACCESO_DENEGADO,
                () -> ejb.listar(new FiltroPermisos(null, null, EstadoPermiso.PENDIENTE), new Paginacion(0, 10)));
    }

    @Test
    void elAltaPideFechaDeInicio() {
        assertCodigo(CodigoError.PERMISO_INICIO_REQUERIDO,
                () -> ejb.addPermiso(new SolicitudPermisoDto("PNC-2", null, 10L)));
        assertTrue(permisos.isEmpty());
    }
}
