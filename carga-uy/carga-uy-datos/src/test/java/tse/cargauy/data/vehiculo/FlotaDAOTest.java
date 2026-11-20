package tse.cargauy.data.vehiculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.data.BaseDatos;
import tse.cargauy.data.empresa.EmpresaDAO;
import tse.cargauy.data.itv.ItvDAO;
import tse.cargauy.data.permiso.PermisoDAO;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.dtos.FiltroPermisos;
import tse.cargauy.dtos.FiltroVehiculos;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.EstadoPermiso;

// vehiculos, permisos e ITV sobre la flota de la demo: STA1234 tiene un permiso vencido (2024-2025) y uno vigente en
// 2026; SDD3456 tiene la ITV y el permiso vencidos en 2026
class FlotaDAOTest extends BaseDatos {

    private static final Paginacion TODO = new Paginacion(0, 100);

    private VehiculoDAO vehiculos;
    private PermisoDAO permisos;
    private ItvDAO itvs;
    private Long empresaSur;
    private VehiculoDto sta;

    @BeforeEach
    void preparar() {
        vehiculos = bean(VehiculoDAO.class);
        permisos = bean(PermisoDAO.class);
        itvs = bean(ItvDAO.class);
        empresaSur = bean(EmpresaDAO.class).getEmpresaByNro(1001).getId();
        sta = vehiculos.getVehiculoByMatricula("STA1234");
    }

    @Test
    void consultaVehiculos() {
        assertEquals("STA1234", vehiculos.getVehiculoById(sta.getId()).getMatricula());
        assertNull(vehiculos.getVehiculoById(-1L));
        assertNull(vehiculos.getVehiculoByMatricula("NOEXISTE"));
    }

    // asignar viaje solo ofrece los vehiculos con capacidad para la carga de la Guia
    @Test
    void losDeLaEmpresaConCapacidadMinima() {
        PaginaDto<VehiculoDto> todos = vehiculos.getByEmpresa(new FiltroVehiculos(empresaSur, null), TODO);
        PaginaDto<VehiculoDto> grandes = vehiculos.getByEmpresa(new FiltroVehiculos(empresaSur, 25000), TODO);

        assertTrue(todos.getItems().stream().anyMatch(v -> v.getMatricula().equals("STA1234")));
        assertTrue(grandes.getItems().stream().allMatch(v -> v.getCapacidadCarga() >= 25000));
        assertTrue(grandes.getItems().stream().noneMatch(v -> v.getMatricula().equals("STA1234")), "STA1234 carga 24000");
        assertTrue(todos.getTotal() > grandes.getTotal());
        assertTrue(todos.getItems().stream().allMatch(v -> v.getIdEmpresa().equals(empresaSur)));
    }

    @Test
    void modificaYBorraUnVehiculo() {
        vehiculos.addVehiculo(new VehiculoDto("SZZ0001", "DAF", "XF", 8000, 22000, empresaSur));
        Long id = vehiculos.getVehiculoByMatricula("SZZ0001").getId();

        vehiculos.updateVehiculo(id, new VehiculoDto("SZZ0002", "DAF", "XG", 8100, 23000, empresaSur));
        sincronizar();
        VehiculoDto modificado = vehiculos.getVehiculoById(id);
        assertEquals("SZZ0002", modificado.getMatricula());
        assertEquals("XG", modificado.getModelo());
        assertEquals(23000, modificado.getCapacidadCarga());

        vehiculos.deleteVehiculo(id);
        vehiculos.deleteVehiculo(-1L);
        sincronizar();
        assertNull(vehiculos.getVehiculoById(id));
    }

    // solo un permiso aprobado y vigente en la fecha habilita al vehiculo (DA11)
    @Test
    void permisoVigenteEnUnaFecha() {
        assertEquals("PNC-1002", permisos.getVigente(sta.getId(), LocalDate.of(2026, 6, 1)).getNroPermiso());
        assertEquals("PNC-1001", permisos.getVigente(sta.getId(), LocalDate.of(2025, 6, 1)).getNroPermiso());
        assertNull(permisos.getVigente(sta.getId(), LocalDate.of(2023, 6, 1)));
    }

    @Test
    void unPendienteNoHabilita() {
        permisos.addPermiso(new PermisoDto("PNC-9001", LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31), sta.getId(),
                EstadoPermiso.PENDIENTE));
        sincronizar();

        assertNull(permisos.getVigente(sta.getId(), LocalDate.of(2028, 6, 1)));
        assertEquals(EstadoPermiso.PENDIENTE, permisos.getPermisoByNro("PNC-9001").getEstado());
    }

    @Test
    void superposicionDeFechas() {
        Long vigente = permisos.getPermisoByNro("PNC-1002").getId();

        assertEquals("PNC-1002", permisos.getSuperpuesto(sta.getId(), LocalDate.of(2026, 12, 1), LocalDate.of(2027, 6, 1), null)
                .getNroPermiso());
        assertNull(permisos.getSuperpuesto(sta.getId(), LocalDate.of(2026, 12, 1), LocalDate.of(2027, 6, 1), vigente),
                "al revisar un permiso no se compara consigo mismo");
        assertNull(permisos.getSuperpuesto(sta.getId(), LocalDate.of(2030, 1, 1), LocalDate.of(2030, 12, 31), null));
    }

    @Test
    void listadosDePermisos() {
        Long scc = vehiculos.getVehiculoByMatricula("SCC9012").getId();

        PaginaDto<PermisoDto> todos = permisos.getAll(TODO);
        PaginaDto<PermisoDto> deSta = permisos.getByVehiculo(sta.getId(), TODO);
        PaginaDto<PermisoDto> filtrados = permisos.getAll(new FiltroPermisos(null, sta.getId(), EstadoPermiso.APROBADO), TODO);
        PaginaDto<PermisoDto> deLaEmpresa = permisos.getAll(new FiltroPermisos(empresaSur, null, null), TODO);
        PaginaDto<PermisoDto> sinFiltro = permisos.getAll(new FiltroPermisos(), new Paginacion(0, 1));

        assertTrue(todos.getTotal() >= 5);
        assertEquals(2, deSta.getTotal());
        assertEquals("PNC-1002", deSta.getItems().get(0).getNroPermiso(), "el mas reciente primero");
        assertEquals(2, filtrados.getTotal());
        assertTrue(deLaEmpresa.getItems().stream().noneMatch(p -> p.getIdVehiculo().equals(scc)));
        assertEquals(todos.getTotal(), sinFiltro.getTotal());
        assertEquals(1, sinFiltro.getItems().size());
        assertEquals(0, permisos.getAll(new FiltroPermisos(null, sta.getId(), EstadoPermiso.RECHAZADO), TODO).getTotal());
    }

    @Test
    void ciclodeRevisionDeUnPermiso() {
        Long funcionario = bean(UsuarioDAO.class).getFuncionarioByCedula("11111111").getId();
        LocalDateTime revision = LocalDateTime.of(2026, 11, 1, 10, 0);
        permisos.addPermiso(new PermisoDto("PNC-9002", LocalDate.of(2027, 3, 1), null, sta.getId(), EstadoPermiso.PENDIENTE));
        Long id = permisos.getPermisoByNro("PNC-9002").getId();

        permisos.rechazar(id, "numero equivocado", funcionario, revision);
        sincronizar();
        PermisoDto rechazado = permisos.getPermisoById(id);
        assertEquals(EstadoPermiso.RECHAZADO, rechazado.getEstado());
        assertEquals("numero equivocado", rechazado.getMotivoRechazo());
        assertEquals("11111111", rechazado.getCedulaRevisor());

        // reenviado: vuelve a pendiente y pierde la revision anterior
        permisos.updatePermiso(id, new PermisoDto("PNC-9003", LocalDate.of(2027, 3, 1), null, sta.getId(), EstadoPermiso.PENDIENTE));
        sincronizar();
        PermisoDto reenviado = permisos.getPermisoById(id);
        assertEquals("PNC-9003", reenviado.getNroPermiso());
        assertNull(reenviado.getMotivoRechazo());
        assertNull(reenviado.getCedulaRevisor());

        permisos.aprobar(id, LocalDate.of(2027, 3, 1), LocalDate.of(2028, 2, 28), funcionario, revision);
        sincronizar();
        PermisoDto aprobado = permisos.getPermisoById(id);
        assertEquals(EstadoPermiso.APROBADO, aprobado.getEstado());
        assertEquals(LocalDate.of(2028, 2, 28), aprobado.getValidoHasta());
        assertEquals(revision, aprobado.getFechaRevision());
        assertNotNull(permisos.getVigente(sta.getId(), LocalDate.of(2027, 6, 1)));

        permisos.deletePermiso(id);
        permisos.deletePermiso(-1L);
        permisos.updatePermiso(-1L, reenviado);
        sincronizar();
        assertNull(permisos.getPermisoById(id));
        assertNull(permisos.getPermisoByNro("PNC-9003"));
    }

    @Test
    void itvVigenteYVencida() {
        Long sdd = vehiculos.getVehiculoByMatricula("SDD3456").getId();

        assertNotNull(itvs.getVigente(sta.getId(), LocalDate.of(2026, 10, 15)));
        assertNull(itvs.getVigente(sdd, LocalDate.of(2026, 10, 15)), "la de SDD3456 vencio el 10/04/2026");
        assertNotNull(itvs.getVigente(sdd, LocalDate.of(2026, 1, 1)));
        assertNull(itvs.getVigente(sta.getId(), LocalDate.of(2020, 1, 1)));
    }

    @Test
    void altaListadoYBajaDeItv() {
        itvs.addItv(new ItvDto(sta.getId(), LocalDate.of(2027, 1, 10), LocalDate.of(2028, 1, 10)));
        sincronizar();

        List<ItvDto> deSta = itvs.getByVehiculo(sta.getId());
        assertEquals(2, deSta.size());
        ItvDto nueva = deSta.stream().filter(i -> i.getFechaInspeccion().equals(LocalDate.of(2027, 1, 10))).findFirst().orElseThrow();
        assertEquals(LocalDate.of(2028, 1, 10), itvs.getItvById(nueva.getId()).getFechaVencimiento());
        assertTrue(itvs.getAll(TODO).getTotal() >= 5);

        itvs.deleteItv(nueva.getId());
        itvs.deleteItv(-1L);
        sincronizar();
        assertNull(itvs.getItvById(nueva.getId()));
        assertEquals(1, itvs.getByVehiculo(sta.getId()).size());
    }
}
