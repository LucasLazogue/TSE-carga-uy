package tse.cargauy.negocio.viaje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.data.eventoviaje.EventoViajeDAO;
import tse.cargauy.data.guia.GuiaDAO;
import tse.cargauy.data.itv.ItvDAO;
import tse.cargauy.data.permiso.PermisoDAO;
import tse.cargauy.data.posicion.EventoPosicionDAO;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.data.viaje.ViajeDAO;
import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.PesajesViajeDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJB;

class ViajeEJBTest {

    private static final Long EMPRESA = 10L;
    private static final Long OTRA_EMPRESA = 20L;
    private static final Long VIAJE = 1L;
    private static final Long GUIA = 2L;
    private static final Paginacion PAGINA = new Paginacion(0, 20);

    private final ViajeEJB ejb = new ViajeEJB();
    private final EventoViajeDto evento = new EventoViajeDto();

    @BeforeEach
    void setUp() {
        ejb.empresaEJB = new EmpresaEJB() {
            @Override
            public void validarAcceso(Long idEmpresa) {
            }
        };
        ejb.viajeDAO = new ViajeDAO() {
            @Override
            public ViajeDto getViajeById(Long id) {
                if (!VIAJE.equals(id)) {
                    return null;
                }
                ViajeDto viaje = new ViajeDto();
                viaje.setIdGuia(GUIA);
                return viaje;
            }
        };
        ejb.guiaDAO = new GuiaDAO() {
            @Override
            public GuiaDto getGuiaById(Long id) {
                GuiaDto guia = new GuiaDto();
                guia.setIdEmpresa(EMPRESA);
                return guia;
            }
        };
        ejb.eventoViajeDAO = new EventoViajeDAO() {
            @Override
            public PaginaDto<EventoViajeDto> getByViaje(Long idViaje, Paginacion paginacion) {
                return new PaginaDto<>(List.of(evento), 1, paginacion.getPagina(), paginacion.getTamanio());
            }
        };
    }

    @Test
    void devuelveLosEventosDelViaje() {
        assertEquals(List.of(evento), ejb.getEventos(EMPRESA, VIAJE, PAGINA).getItems());
    }

    @Test
    void noDevuelveEventosDeUnViajeDeOtraEmpresa() {
        CargaUYException error = assertThrows(CargaUYException.class, () -> ejb.getEventos(OTRA_EMPRESA, VIAJE, PAGINA));
        assertEquals(CodigoError.VIAJE_NO_ENCONTRADO, error.getCodigo());
    }

    @Test
    void noDevuelveEventosDeUnViajeInexistente() {
        CargaUYException error = assertThrows(CargaUYException.class, () -> ejb.getEventos(EMPRESA, 99L, PAGINA));
        assertEquals(CodigoError.VIAJE_NO_ENCONTRADO, error.getCodigo());
    }

    @Test
    void devuelveLosPesajesDelViajeConsultandoALasBalanzas() {
        PesajesViajeDto pesajes = new PesajesViajeDto();
        ejb.balanzas = viaje -> {
            assertEquals(GUIA, viaje.getIdGuia());
            return pesajes;
        };

        assertSame(pesajes, ejb.getPesajes(EMPRESA, VIAJE));
    }

    @Test
    void noConsultaPesajesDeUnViajeDeOtraEmpresa() {
        ejb.balanzas = viaje -> {
            throw new AssertionError("no tenia que consultar las balanzas");
        };

        CargaUYException error = assertThrows(CargaUYException.class, () -> ejb.getPesajes(OTRA_EMPRESA, VIAJE));
        assertEquals(CodigoError.VIAJE_NO_ENCONTRADO, error.getCodigo());
    }

    // el dia de la baja el vinculo sigue vigente (getChoferVigente lo encuentra), pero ya tiene baja
    @Test
    void noAsignaNiReasignaAUnChoferDadoDeBajaHoy() {
        LocalDate hoy = LocalDate.now();
        ejb.guiaDAO = new GuiaDAO() {
            @Override
            public GuiaDto getGuiaById(Long id) {
                GuiaDto guia = new GuiaDto();
                guia.setId(GUIA);
                guia.setIdEmpresa(EMPRESA);
                guia.setFecha(hoy);
                guia.setVolumen(1000);
                return guia;
            }
        };
        ejb.viajeDAO = new ViajeDAO() {
            @Override
            public ViajeDto getByGuia(Long idGuia) {
                return null;
            }

            @Override
            public ViajeDto getViajeById(Long id) {
                ViajeDto viaje = new ViajeDto();
                viaje.setIdGuia(GUIA);
                viaje.setEstado(EstadoViaje.ASIGNADO);
                return viaje;
            }
        };
        ejb.vehiculoDAO = new VehiculoDAO() {
            @Override
            public VehiculoDto getVehiculoById(Long id) {
                VehiculoDto vehiculo = new VehiculoDto("STA1234", "Volvo", "FH", 15000, 30000, EMPRESA);
                vehiculo.setId(id);
                return vehiculo;
            }
        };
        ejb.permisoDAO = new PermisoDAO() {
            @Override
            public PermisoDto getVigente(Long idVehiculo, LocalDate fecha) {
                return new PermisoDto();
            }
        };
        ejb.itvDAO = new ItvDAO() {
            @Override
            public ItvDto getVigente(Long idVehiculo, LocalDate fecha) {
                return new ItvDto();
            }
        };
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public Long getChoferVigente(Long idCiudadano, Long idEmpresa, LocalDate fecha) {
                return 1L;
            }

            @Override
            public Long getChoferSinBaja(Long idCiudadano, Long idEmpresa, LocalDate fecha) {
                return null;
            }
        };
        ViajeDto datos = new ViajeDto();
        datos.setIdGuia(GUIA);
        datos.setIdVehiculo(3L);
        datos.setIdChofer(5L);

        CargaUYException asignar = assertThrows(CargaUYException.class, () -> ejb.asignarViaje(EMPRESA, datos));
        CargaUYException reasignar = assertThrows(CargaUYException.class, () -> ejb.reasignarViaje(EMPRESA, VIAJE, datos));

        assertEquals(CodigoError.VIAJE_CHOFER_INVALIDO, asignar.getCodigo());
        assertEquals(CodigoError.VIAJE_CHOFER_INVALIDO, reasignar.getCodigo());
    }

    @Test
    void devuelveLasPosicionesDelViaje() {
        EventoPosicionDto posicion = new EventoPosicionDto();
        ejb.posicionDAO = new EventoPosicionDAO() {
            @Override
            public List<EventoPosicionDto> getRutaDelViaje(Long idViaje) {
                assertEquals(VIAJE, idViaje);
                return List.of(posicion);
            }
        };

        assertEquals(List.of(posicion), ejb.getPosiciones(EMPRESA, VIAJE));
    }
}
