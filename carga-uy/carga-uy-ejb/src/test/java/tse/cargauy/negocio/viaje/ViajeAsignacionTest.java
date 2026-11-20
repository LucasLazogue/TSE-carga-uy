package tse.cargauy.negocio.viaje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.data.guia.GuiaDAO;
import tse.cargauy.data.itv.ItvDAO;
import tse.cargauy.data.permiso.PermisoDAO;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.data.viaje.ViajeDAO;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJB;

// asignacion, reasignacion y baja de viajes: las reglas que habilitan a un vehiculo y a un chofer para una Guia
class ViajeAsignacionTest {

    private static final Long EMPRESA = 10L;
    private static final Long OTRA_EMPRESA = 20L;
    private static final Long GUIA = 2L;
    private static final Long VIAJE = 1L;
    private static final Long VEHICULO = 3L;
    private static final Long CHOFER = 5L;
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 1);

    private final ViajeEJB ejb = new ViajeEJB();
    private final List<String> llamadas = new ArrayList<>();

    private GuiaDto guia;
    private VehiculoDto vehiculo;
    private ViajeDto viajeDeLaGuia;
    private EstadoViaje estado = EstadoViaje.ASIGNADO;
    private boolean conPermiso = true;
    private boolean conItv = true;
    private boolean choferHabilitado = true;

    @BeforeEach
    void setUp() {
        guia = new GuiaDto();
        guia.setId(GUIA);
        guia.setNroGuia("G-2");
        guia.setIdEmpresa(EMPRESA);
        guia.setFecha(FECHA);
        guia.setVolumen(20000);
        vehiculo = new VehiculoDto("SBA1234", "Scania", "R450", 9000, 25000, EMPRESA);
        vehiculo.setId(VEHICULO);

        ejb.empresaEJB = new EmpresaEJB() {
            @Override
            public void validarAcceso(Long idEmpresa) {
            }
        };
        ejb.guiaDAO = new GuiaDAO() {
            @Override
            public GuiaDto getGuiaById(Long id) {
                return GUIA.equals(id) ? guia : null;
            }
        };
        ejb.vehiculoDAO = new VehiculoDAO() {
            @Override
            public VehiculoDto getVehiculoById(Long id) {
                return VEHICULO.equals(id) ? vehiculo : null;
            }
        };
        ejb.permisoDAO = new PermisoDAO() {
            @Override
            public PermisoDto getVigente(Long idVehiculo, LocalDate fecha) {
                assertEquals(FECHA, fecha, "el permiso tiene que estar vigente en la fecha de la Guia");
                return conPermiso ? new PermisoDto() : null;
            }
        };
        ejb.itvDAO = new ItvDAO() {
            @Override
            public ItvDto getVigente(Long idVehiculo, LocalDate fecha) {
                return conItv ? new ItvDto() : null;
            }
        };
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public Long getChoferSinBaja(Long idCiudadano, Long idEmpresa, LocalDate fecha) {
                return choferHabilitado ? idCiudadano : null;
            }
        };
        ejb.viajeDAO = new ViajeDAO() {
            @Override
            public ViajeDto getByGuia(Long idGuia) {
                return viajeDeLaGuia;
            }

            @Override
            public ViajeDto getViajeById(Long id) {
                if (!VIAJE.equals(id)) {
                    return null;
                }
                ViajeDto viaje = new ViajeDto();
                viaje.setId(VIAJE);
                viaje.setIdGuia(GUIA);
                viaje.setEstado(estado);
                return viaje;
            }

            @Override
            public PaginaDto<ViajeDto> getAll(FiltroViajes filtro, Paginacion paginacion) {
                llamadas.add("getAll " + filtro.getIdsEmpresa());
                return new PaginaDto<>(List.of(), 0, paginacion.getPagina(), paginacion.getTamanio());
            }

            @Override
            public ViajeDto addViaje(ViajeDto viajeDto) {
                llamadas.add("add");
                return viajeDto;
            }

            @Override
            public void updateViaje(Long id, ViajeDto viajeDto) {
                llamadas.add("update " + id);
            }

            @Override
            public void deleteViaje(Long id) {
                llamadas.add("delete " + id);
            }
        };
    }

    private static ViajeDto datos() {
        ViajeDto viaje = new ViajeDto();
        viaje.setIdGuia(GUIA);
        viaje.setIdVehiculo(VEHICULO);
        viaje.setIdChofer(CHOFER);
        return viaje;
    }

    private void assertRechazada(CodigoError esperado, ViajeDto datos) {
        CargaUYException e = assertThrows(CargaUYException.class, () -> ejb.asignarViaje(EMPRESA, datos));
        assertEquals(esperado, e.getCodigo());
        assertTrue(llamadas.isEmpty(), "no debe guardar el viaje");
    }

    @Test
    void listarFiltraPorLaEmpresa() {
        ejb.listar(EMPRESA, new FiltroViajes(), new Paginacion(0, 20));

        assertEquals(List.of("getAll " + List.of(EMPRESA)), llamadas);
    }

    @Test
    void getViajeDeLaEmpresa() {
        assertEquals(VIAJE, ejb.getViaje(EMPRESA, VIAJE).getId());
        CargaUYException e = assertThrows(CargaUYException.class, () -> ejb.getViaje(OTRA_EMPRESA, VIAJE));
        assertEquals(CodigoError.VIAJE_NO_ENCONTRADO, e.getCodigo());
    }

    @Test
    void asignaUnViajeValido() {
        ViajeDto datos = datos();

        assertSame(datos, ejb.asignarViaje(EMPRESA, datos));
        assertEquals(List.of("add"), llamadas);
    }

    // la carga entra justo en la capacidad del vehiculo
    @Test
    void asignaConLaCapacidadJusta() {
        guia.setVolumen(vehiculo.getCapacidadCarga());

        ejb.asignarViaje(EMPRESA, datos());

        assertEquals(List.of("add"), llamadas);
    }

    @Test
    void sinGuiaNoAsigna() {
        ViajeDto datos = datos();
        datos.setIdGuia(null);

        assertRechazada(CodigoError.VIAJE_GUIA_REQUERIDA, datos);
    }

    @Test
    void unaGuiaInexistenteODeOtraEmpresa() {
        ViajeDto inexistente = datos();
        inexistente.setIdGuia(99L);
        assertRechazada(CodigoError.GUIA_NO_ENCONTRADA, inexistente);

        guia.setIdEmpresa(OTRA_EMPRESA);
        assertRechazada(CodigoError.GUIA_NO_ENCONTRADA, datos());
    }

    @Test
    void unaGuiaNoAmparaDosViajes() {
        viajeDeLaGuia = new ViajeDto();

        assertRechazada(CodigoError.VIAJE_GUIA_YA_ASIGNADA, datos());
    }

    @Test
    void requiereVehiculoYChofer() {
        ViajeDto sinVehiculo = datos();
        sinVehiculo.setIdVehiculo(null);
        ViajeDto sinChofer = datos();
        sinChofer.setIdChofer(null);
        ViajeDto vehiculoInexistente = datos();
        vehiculoInexistente.setIdVehiculo(99L);

        assertRechazada(CodigoError.VIAJE_VEHICULO_REQUERIDO, sinVehiculo);
        assertRechazada(CodigoError.VIAJE_CHOFER_REQUERIDO, sinChofer);
        assertRechazada(CodigoError.VEHICULO_NO_ENCONTRADO, vehiculoInexistente);
    }

    @Test
    void elVehiculoTieneQueSerDeLaEmpresaDeLaGuia() {
        vehiculo.setIdEmpresa(OTRA_EMPRESA);

        assertRechazada(CodigoError.VIAJE_VEHICULO_OTRA_EMPRESA, datos());
    }

    @Test
    void laCargaNoPuedeSuperarLaCapacidad() {
        guia.setVolumen(vehiculo.getCapacidadCarga() + 1);

        assertRechazada(CodigoError.VIAJE_CAPACIDAD_EXCEDIDA, datos());
    }

    @Test
    void elVehiculoNecesitaPermisoEItvVigentes() {
        conPermiso = false;
        assertRechazada(CodigoError.VIAJE_VEHICULO_SIN_PERMISO, datos());

        conPermiso = true;
        conItv = false;
        assertRechazada(CodigoError.VIAJE_VEHICULO_SIN_ITV, datos());
    }

    @Test
    void elChoferTieneQueEstarHabilitado() {
        choferHabilitado = false;

        assertRechazada(CodigoError.VIAJE_CHOFER_INVALIDO, datos());
    }

    @Test
    void reasignaUnViajeAsignado() {
        ViajeDto reasignado = ejb.reasignarViaje(EMPRESA, VIAJE, datos());

        assertEquals(VIAJE, reasignado.getId());
        assertEquals(List.of("update " + VIAJE), llamadas);
    }

    @Test
    void reasignarValidaLaNuevaAsignacion() {
        conItv = false;

        CargaUYException e = assertThrows(CargaUYException.class, () -> ejb.reasignarViaje(EMPRESA, VIAJE, datos()));
        assertEquals(CodigoError.VIAJE_VEHICULO_SIN_ITV, e.getCodigo());
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void borraUnViajeAsignado() {
        ejb.deleteViaje(EMPRESA, VIAJE);

        assertEquals(List.of("delete " + VIAJE), llamadas);
    }

    // un viaje que ya empezo o termino no se reasigna ni se borra
    @Test
    void unViajeIniciadoOFinalizadoNoSeModifica() {
        for (EstadoViaje noModificable : List.of(EstadoViaje.EN_CURSO, EstadoViaje.FINALIZADO)) {
            estado = noModificable;

            CargaUYException reasignar = assertThrows(CargaUYException.class, () -> ejb.reasignarViaje(EMPRESA, VIAJE, datos()));
            CargaUYException borrar = assertThrows(CargaUYException.class, () -> ejb.deleteViaje(EMPRESA, VIAJE));

            assertEquals(CodigoError.VIAJE_NO_MODIFICABLE, reasignar.getCodigo());
            assertEquals(CodigoError.VIAJE_NO_MODIFICABLE, borrar.getCodigo());
        }
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void noBorraUnViajeDeOtraEmpresa() {
        CargaUYException e = assertThrows(CargaUYException.class, () -> ejb.deleteViaje(OTRA_EMPRESA, VIAJE));

        assertEquals(CodigoError.VIAJE_NO_ENCONTRADO, e.getCodigo());
        assertTrue(llamadas.isEmpty());
    }
}
