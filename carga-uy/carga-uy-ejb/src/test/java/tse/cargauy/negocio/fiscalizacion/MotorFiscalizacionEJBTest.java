package tse.cargauy.negocio.fiscalizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.data.permiso.PermisoDAO;
import tse.cargauy.data.posicion.EventoPosicionDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.data.viaje.ViajeDAO;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.TipoEvidencia;
import tse.cargauy.entities.TipoInfraccion;

class MotorFiscalizacionEJBTest {

    private static final Long VEHICULO = 11L;
    private static final Instant GENERADA = Instant.parse("2026-10-06T12:00:00Z");
    private static final LocalDate DIA = LocalDateTime.ofInstant(GENERADA, ZoneId.systemDefault()).toLocalDate();

    private final MotorFiscalizacionEJB motor = new MotorFiscalizacionEJB();
    private final CasosEnMemoria casos = new CasosEnMemoria();
    private final EventoPosicionDto posicion = new EventoPosicionDto();
    private ViajeDto viajeEnCurso;
    private ViajeDto viajeAsignado;
    private ViajeDto viaje;
    private PermisoDto permiso = new PermisoDto();

    @BeforeEach
    void preparar() {
        posicion.setIdNodo(3L);
        posicion.setNodo("tracking-1");
        posicion.setIdEvento("ev-1");
        posicion.setIdVehiculo(VEHICULO);
        posicion.setLatitud(-34.9);
        posicion.setLongitud(-56.2);
        posicion.setTimestampGeneracion(GENERADA);

        motor.eventoDAO = new EventoPosicionDAO() {
            @Override
            public EventoPosicionDto get(Long idNodo, String idEvento) {
                return posicion;
            }
        };
        motor.viajeDAO = new ViajeDAO() {
            @Override
            public ViajeDto getViajeById(Long id) {
                return viaje;
            }

            @Override
            public ViajeDto getEnCursoDelVehiculo(Long idVehiculo, LocalDateTime momento) {
                return viajeEnCurso;
            }

            @Override
            public PaginaDto<ViajeDto> getAll(FiltroViajes filtro, Paginacion paginacion) {
                List<ViajeDto> items = viajeAsignado == null ? List.of() : List.of(viajeAsignado);
                return new PaginaDto<>(items, items.size(), paginacion.getPagina(), paginacion.getTamanio());
            }
        };
        motor.permisoDAO = new PermisoDAO() {
            @Override
            public PermisoDto getVigente(Long idVehiculo, LocalDate fecha) {
                return permiso;
            }
        };

        CasoFiscalizacionEJB casoEJB = new CasoFiscalizacionEJB() {
            @Override
            LocalDateTime ahora() {
                return LocalDateTime.of(2026, 10, 6, 13, 0);
            }
        };
        casoEJB.casoDAO = casos;
        casoEJB.vehiculoDAO = new VehiculoDAO() {
            @Override
            public VehiculoDto getVehiculoById(Long id) {
                return new VehiculoDto();
            }
        };
        motor.casoEJB = casoEJB;
    }

    private List<CasoFiscalizacionDto> casosDe(TipoInfraccion tipo) {
        return casos.casos.values().stream().filter(c -> c.getTipo() == tipo).toList();
    }

    private static ViajeDto viaje(EstadoViaje estado, LocalDateTime inicio) {
        ViajeDto viaje = new ViajeDto();
        viaje.setId(21L);
        viaje.setIdVehiculo(VEHICULO);
        viaje.setEstado(estado);
        viaje.setFechaInicio(inicio);
        return viaje;
    }

    @Test
    void unaPosicionSinViajeGeneraUnCasoDeViajeSinGuia() {
        motor.evaluarPosicion(3L, "ev-1");

        List<CasoFiscalizacionDto> sinGuia = casosDe(TipoInfraccion.VIAJE_SIN_GUIA);
        assertEquals(1, sinGuia.size());
        assertEquals("VIAJE_SIN_GUIA|11|" + DIA, sinGuia.get(0).getClaveDeduplicacion());
        EvidenciaDto evidencia = sinGuia.get(0).getEvidencias().get(0);
        assertEquals(TipoEvidencia.POSICION, evidencia.getTipo());
        assertEquals("tracking-1/ev-1", evidencia.getIdOrigen());
        assertEquals(1, casos.casos.size());
    }

    // el inicio del viaje puede no haber llegado todavia desde el movil
    @Test
    void conUnViajeAsignadoNoHayCaso() {
        viajeAsignado = viaje(EstadoViaje.ASIGNADO, null);

        motor.evaluarPosicion(3L, "ev-1");

        assertTrue(casos.casos.isEmpty());
    }

    @Test
    void conUnViajeEnCursoNoHayViajeSinGuia() {
        viajeEnCurso = viaje(EstadoViaje.EN_CURSO, LocalDateTime.of(2026, 10, 6, 8, 0));

        motor.evaluarPosicion(3L, "ev-1");

        assertTrue(casos.casos.isEmpty());
    }

    @Test
    void unVehiculoSinPermisoEseDiaGeneraUnCasoSinHabilitacion() {
        viajeEnCurso = viaje(EstadoViaje.EN_CURSO, LocalDateTime.of(2026, 10, 6, 8, 0));
        permiso = null;

        motor.evaluarPosicion(3L, "ev-1");

        List<CasoFiscalizacionDto> sinHabilitacion = casosDe(TipoInfraccion.SIN_HABILITACION);
        assertEquals(1, sinHabilitacion.size());
        assertEquals("SIN_HABILITACION|11|" + DIA, sinHabilitacion.get(0).getClaveDeduplicacion());
        assertEquals(TipoEvidencia.PERMISO, sinHabilitacion.get(0).getEvidencias().get(0).getTipo());
        assertNull(sinHabilitacion.get(0).getIdViaje());
        assertEquals(1, casos.casos.size());
    }

    @Test
    void laMismaPosicionDosVecesNoDuplicaCasos() {
        permiso = null;

        motor.evaluarPosicion(3L, "ev-1");
        motor.evaluarPosicion(3L, "ev-1");

        assertEquals(1, casosDe(TipoInfraccion.VIAJE_SIN_GUIA).size());
        assertEquals(1, casosDe(TipoInfraccion.SIN_HABILITACION).size());
        assertEquals(2, casos.casos.size());
    }

    @Test
    void elViajeSeEvaluaRecienCuandoTermina() {
        permiso = null;
        viaje = viaje(EstadoViaje.EN_CURSO, LocalDateTime.of(2026, 10, 1, 8, 0));

        motor.evaluarViaje(21L);
        assertTrue(casos.casos.isEmpty());

        viaje.setEstado(EstadoViaje.FINALIZADO);
        motor.evaluarViaje(21L);

        List<CasoFiscalizacionDto> sinHabilitacion = casosDe(TipoInfraccion.SIN_HABILITACION);
        assertEquals(1, sinHabilitacion.size());
        assertEquals("SIN_HABILITACION|11|2026-10-01", sinHabilitacion.get(0).getClaveDeduplicacion());
        assertEquals(21L, sinHabilitacion.get(0).getIdViaje());
        assertEquals("viaje 21", sinHabilitacion.get(0).getEvidencias().get(0).getIdOrigen());
    }

    @Test
    void unaMatriculaDesconocidaNoSeEvalua() {
        posicion.setIdVehiculo(null);
        permiso = null;

        motor.evaluarPosicion(3L, "ev-1");

        assertTrue(casos.casos.isEmpty());
    }
}
