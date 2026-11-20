package tse.cargauy.negocio.permiso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import jakarta.ejb.SessionContext;
import tse.cargauy.data.permiso.PermisoDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.dtos.FiltroPermisos;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.SolicitudPermisoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.EstadoPermiso;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJB;

// consultas de permisos y control de acceso por empresa del vehiculo (R9)
class PermisoConsultasTest {

    private static final Long VEHICULO = 10L;
    private static final Long VEHICULO_AJENO = 11L;
    private static final Long EMPRESA = 1L;
    private static final Long EMPRESA_AJENA = 2L;
    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final Paginacion PAGINA = new Paginacion(0, 10);

    private final PermisoEJB ejb = new PermisoEJB();
    private final List<String> llamadas = new ArrayList<>();
    private final PermisoDto existente = new PermisoDto("PNC-1", DESDE, null, VEHICULO, EstadoPermiso.PENDIENTE);
    private final PaginaDto<PermisoDto> pagina = new PaginaDto<>(List.of(), 0, 0, 10);

    private boolean funcionario;

    @BeforeEach
    void iniciar() {
        existente.setId(1L);
        ejb.contexto = (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { SessionContext.class },
                (proxy, metodo, args) -> metodo.getName().equals("isCallerInRole") ? funcionario : null);
        ejb.empresaEJB = new EmpresaEJB() {
            @Override
            public void validarAcceso(Long idEmpresa) {
                llamadas.add("acceso " + idEmpresa);
                if (EMPRESA_AJENA.equals(idEmpresa)) {
                    throw new CargaUYException(CodigoError.ACCESO_DENEGADO);
                }
            }
        };
        ejb.vehiculoDAO = new VehiculoDAO() {
            @Override
            public VehiculoDto getVehiculoById(Long id) {
                if (!VEHICULO.equals(id) && !VEHICULO_AJENO.equals(id)) {
                    return null;
                }
                VehiculoDto vehiculo = new VehiculoDto();
                vehiculo.setId(id);
                vehiculo.setIdEmpresa(VEHICULO.equals(id) ? EMPRESA : EMPRESA_AJENA);
                return vehiculo;
            }
        };
        ejb.permisoDAO = new PermisoDAO() {
            @Override
            public PermisoDto getPermisoById(Long id) {
                return Long.valueOf(1L).equals(id) ? existente : null;
            }

            @Override
            public PermisoDto getPermisoByNro(String nroPermiso) {
                return existente.getNroPermiso().equals(nroPermiso) ? existente : null;
            }

            @Override
            public PaginaDto<PermisoDto> getAll(Paginacion paginacion) {
                llamadas.add("getAll");
                return pagina;
            }

            @Override
            public PaginaDto<PermisoDto> getAll(FiltroPermisos filtro, Paginacion paginacion) {
                llamadas.add("buscar");
                return pagina;
            }

            @Override
            public PaginaDto<PermisoDto> getByVehiculo(Long idVehiculo, Paginacion paginacion) {
                llamadas.add("getByVehiculo " + idVehiculo);
                return pagina;
            }

            @Override
            public PermisoDto getVigente(Long idVehiculo, LocalDate fecha) {
                return existente;
            }

            @Override
            public void addPermiso(PermisoDto permisoDto) {
                llamadas.add("add");
            }

            @Override
            public void updatePermiso(Long id, PermisoDto permisoDto) {
                llamadas.add("update " + id);
            }

            @Override
            public void deletePermiso(Long id) {
                llamadas.add("delete " + id);
            }
        };
    }

    private static void assertCodigo(CodigoError esperado, Executable accion) {
        assertEquals(esperado, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    @Test
    void getPermisoValidaElAccesoAlVehiculo() {
        assertSame(existente, ejb.getPermisoById(1L));
        assertEquals(List.of("acceso " + EMPRESA), llamadas);
        assertNull(ejb.getPermisoById(99L));
    }

    @Test
    void consultasPorVehiculo() {
        assertSame(pagina, ejb.getByVehiculo(VEHICULO, PAGINA));
        assertSame(existente, ejb.getVigente(VEHICULO, DESDE));
        assertCodigo(CodigoError.ACCESO_DENEGADO, () -> ejb.getByVehiculo(VEHICULO_AJENO, PAGINA));
        assertCodigo(CodigoError.ACCESO_DENEGADO, () -> ejb.getVigente(VEHICULO_AJENO, DESDE));
    }

    @Test
    void elFuncionarioVeTodos() {
        assertSame(pagina, ejb.getAll(PAGINA));
    }

    @Test
    void listarPorVehiculoPorEmpresaOComoFuncionario() {
        ejb.listar(new FiltroPermisos(null, VEHICULO, null), PAGINA);
        ejb.listar(new FiltroPermisos(EMPRESA, null, null), PAGINA);
        funcionario = true;
        ejb.listar(new FiltroPermisos(null, null, EstadoPermiso.PENDIENTE), PAGINA);

        assertEquals(List.of("acceso " + EMPRESA, "buscar", "acceso " + EMPRESA, "buscar", "buscar"), llamadas);
    }

    @Test
    void noListaLosDeOtraEmpresa() {
        assertCodigo(CodigoError.ACCESO_DENEGADO, () -> ejb.listar(new FiltroPermisos(null, VEHICULO_AJENO, null), PAGINA));
        assertCodigo(CodigoError.ACCESO_DENEGADO, () -> ejb.listar(new FiltroPermisos(EMPRESA_AJENA, null, null), PAGINA));
        assertTrue(llamadas.stream().noneMatch("buscar"::equals));
    }

    @Test
    void elNumeroDePermisoNoSeRepite() {
        assertCodigo(CodigoError.PERMISO_NRO_DUPLICADO,
                () -> ejb.addPermiso(new SolicitudPermisoDto("PNC-1", DESDE, VEHICULO)));
        assertCodigo(CodigoError.PERMISO_NRO_DUPLICADO,
                () -> ejb.updatePermiso(2L, new SolicitudPermisoDto("PNC-1", DESDE, VEHICULO)));
        assertTrue(llamadas.stream().noneMatch(l -> l.startsWith("add") || l.startsWith("update")));
    }

    // conservar el propio numero al corregir no es un duplicado
    @Test
    void corregirConservandoSuNumero() {
        ejb.updatePermiso(1L, new SolicitudPermisoDto("PNC-1", DESDE.plusDays(1), VEHICULO));

        assertTrue(llamadas.contains("update 1"));
    }

    @Test
    void elAltaValidaLosDatos() {
        assertCodigo(CodigoError.PERMISO_NRO_REQUERIDO, () -> ejb.addPermiso(new SolicitudPermisoDto(null, DESDE, VEHICULO)));
        assertCodigo(CodigoError.PERMISO_NRO_REQUERIDO, () -> ejb.addPermiso(new SolicitudPermisoDto(" ", DESDE, VEHICULO)));
        assertCodigo(CodigoError.PERMISO_VEHICULO_REQUERIDO, () -> ejb.addPermiso(new SolicitudPermisoDto("PNC-9", DESDE, null)));
        assertCodigo(CodigoError.VEHICULO_NO_ENCONTRADO, () -> ejb.addPermiso(new SolicitudPermisoDto("PNC-9", DESDE, 99L)));
        assertCodigo(CodigoError.ACCESO_DENEGADO,
                () -> ejb.addPermiso(new SolicitudPermisoDto("PNC-9", DESDE, VEHICULO_AJENO)));
        assertTrue(llamadas.stream().noneMatch("add"::equals));
    }

    @Test
    void borraUnPendienteYToleraUnoInexistente() {
        ejb.deletePermiso(1L);
        ejb.deletePermiso(99L);

        assertEquals(List.of("acceso " + EMPRESA, "delete 1", "delete 99"), llamadas);
    }
}
