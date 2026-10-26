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

import io.opentelemetry.context.Context;
import tse.cargauy.data.itv.ItvDAO;
import tse.cargauy.data.permiso.PermisoDAO;
import tse.cargauy.data.posicion.EventoPosicionDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.data.viaje.ViajeDAO;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.PesajeDto;
import tse.cargauy.dtos.PesajesViajeDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.TipoEvidencia;
import tse.cargauy.entities.TipoInfraccion;

class MotorFiscalizacionEJBTest {

    private static final Long VEHICULO = 11L;
    private static final Instant GENERADA = Instant.parse("2026-10-06T12:00:00Z");
    private static final LocalDate DIA = LocalDateTime.ofInstant(GENERADA, ZoneId.systemDefault()).toLocalDate();
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 14, 0);
    private static final String SUR = "balanzas-sur/balanza-r5-km30/1001";
    private static final String NORTE = "balanzas-norte/balanza-r8-km60/3003";

    private final MotorFiscalizacionEJB motor = new MotorFiscalizacionEJB() {
        @Override
        LocalDateTime ahora() {
            return AHORA;
        }
    };
    private final CasosEnMemoria casos = new CasosEnMemoria();
    private final EventoPosicionDto posicion = new EventoPosicionDto();
    private final PesajesViajeDto pesajes = new PesajesViajeDto();
    private ViajeDto viajeEnCurso;
    private ViajeDto viajeAsignado;
    private ViajeDto viaje;
    private PermisoDto permiso = new PermisoDto();
    private ItvDto itv = new ItvDto();
    private List<Long> reservados = List.of();
    private LocalDateTime reservadoHasta;
    private LocalDateTime proximaEvaluacion;

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
                List<ViajeDto> items = viajeAsignado != null && filtro.getEstado() == EstadoViaje.ASIGNADO
                        && VEHICULO.equals(filtro.getIdVehiculo()) ? List.of(viajeAsignado) : List.of();
                return new PaginaDto<>(items, items.size(), paginacion.getPagina(), paginacion.getTamanio());
            }

            @Override
            public void marcarEvaluacionCarga(Long id, LocalDateTime proxima) {
                proximaEvaluacion = proxima;
            }

            @Override
            public List<Long> reservarEvaluacionesCarga(LocalDateTime ahora, LocalDateTime hasta, int maximo) {
                reservadoHasta = hasta;
                return reservados;
            }
        };
        motor.permisoDAO = new PermisoDAO() {
            @Override
            public PermisoDto getVigente(Long idVehiculo, LocalDate fecha) {
                return permiso;
            }
        };
        motor.itvDAO = new ItvDAO() {
            @Override
            public ItvDto getVigente(Long idVehiculo, LocalDate fecha) {
                return itv;
            }
        };
        // 15000 kg de peso y 18800 kg de capacidad: el maximo es 33800 kg
        VehiculoDAO vehiculoDAO = new VehiculoDAO() {
            @Override
            public VehiculoDto getVehiculoById(Long id) {
                return new VehiculoDto("SCC9012", "Scania", "R450", 15000, 18800, 1L);
            }
        };
        motor.vehiculoDAO = vehiculoDAO;
        motor.balanzas = consultado -> pesajes;
        motor.casoDAO = casos;

        CasoFiscalizacionEJB casoEJB = new CasoFiscalizacionEJB();
        casoEJB.casoDAO = casos;
        casoEJB.vehiculoDAO = vehiculoDAO;
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

    private static PesajeDto pesaje(String idOrigen, int peso) {
        PesajeDto pesaje = new PesajeDto();
        pesaje.setIdOrigen(idOrigen);
        pesaje.setFechaHora(LocalDateTime.of(2026, 10, 1, 10, 0));
        pesaje.setPesoRegistrado(peso);
        pesaje.setNombrePeaje("Pando");
        return pesaje;
    }

    @Test
    void unaPosicionSinViajeGeneraUnCasoDeViajeSinGuia() {
        motor.evaluarPosicion(3L, "ev-1", Context.root());

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

        motor.evaluarPosicion(3L, "ev-1", Context.root());

        assertTrue(casos.casos.isEmpty());
    }

    @Test
    void conUnViajeEnCursoNoHayViajeSinGuia() {
        viajeEnCurso = viaje(EstadoViaje.EN_CURSO, LocalDateTime.of(2026, 10, 6, 8, 0));

        motor.evaluarPosicion(3L, "ev-1", Context.root());

        assertTrue(casos.casos.isEmpty());
    }

    @Test
    void unVehiculoSinPermisoEseDiaGeneraUnCasoSinHabilitacion() {
        viajeEnCurso = viaje(EstadoViaje.EN_CURSO, LocalDateTime.of(2026, 10, 6, 8, 0));
        permiso = null;

        motor.evaluarPosicion(3L, "ev-1", Context.root());

        List<CasoFiscalizacionDto> sinHabilitacion = casosDe(TipoInfraccion.SIN_HABILITACION);
        assertEquals(1, sinHabilitacion.size());
        assertEquals("SIN_HABILITACION|11|" + DIA, sinHabilitacion.get(0).getClaveDeduplicacion());
        assertEquals(TipoEvidencia.PERMISO, sinHabilitacion.get(0).getEvidencias().get(0).getTipo());
        assertNull(sinHabilitacion.get(0).getIdViaje());
        assertEquals(1, casos.casos.size());
    }

    @Test
    void conPermisoPeroSinItvElCasoTieneLaEvidenciaDeLaItv() {
        viajeEnCurso = viaje(EstadoViaje.EN_CURSO, LocalDateTime.of(2026, 10, 6, 8, 0));
        itv = null;

        motor.evaluarPosicion(3L, "ev-1", Context.root());

        List<CasoFiscalizacionDto> sinHabilitacion = casosDe(TipoInfraccion.SIN_HABILITACION);
        assertEquals(1, sinHabilitacion.size());
        assertEquals("SIN_HABILITACION|11|" + DIA, sinHabilitacion.get(0).getClaveDeduplicacion());
        assertEquals(List.of(TipoEvidencia.ITV),
                sinHabilitacion.get(0).getEvidencias().stream().map(EvidenciaDto::getTipo).toList());
    }

    @Test
    void sinPermisoNiItvHayUnSoloCasoConLasDosEvidencias() {
        viajeEnCurso = viaje(EstadoViaje.EN_CURSO, LocalDateTime.of(2026, 10, 6, 8, 0));
        permiso = null;
        itv = null;

        motor.evaluarPosicion(3L, "ev-1", Context.root());

        List<CasoFiscalizacionDto> sinHabilitacion = casosDe(TipoInfraccion.SIN_HABILITACION);
        assertEquals(1, sinHabilitacion.size());
        assertEquals(List.of(TipoEvidencia.PERMISO, TipoEvidencia.ITV),
                sinHabilitacion.get(0).getEvidencias().stream().map(EvidenciaDto::getTipo).toList());
        assertEquals(1, casos.casos.size());
    }

    @Test
    void laMismaPosicionDosVecesNoDuplicaCasos() {
        permiso = null;

        motor.evaluarPosicion(3L, "ev-1", Context.root());
        motor.evaluarPosicion(3L, "ev-1", Context.root());

        assertEquals(1, casosDe(TipoInfraccion.VIAJE_SIN_GUIA).size());
        assertEquals(1, casosDe(TipoInfraccion.SIN_HABILITACION).size());
        assertEquals(2, casos.casos.size());
    }

    @Test
    void elViajeSeEvaluaRecienCuandoTermina() {
        pesajes.getPesajes().add(pesaje(SUR, 36500));
        permiso = null;
        viaje = viaje(EstadoViaje.EN_CURSO, LocalDateTime.of(2026, 10, 1, 8, 0));

        motor.evaluarViaje(21L, Context.root());
        assertTrue(casos.casos.isEmpty());

        viaje.setEstado(EstadoViaje.FINALIZADO);
        motor.evaluarViaje(21L, Context.root());

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

        motor.evaluarPosicion(3L, "ev-1", Context.root());

        assertTrue(casos.casos.isEmpty());
    }

    @Test
    void unPesajeSobreElMaximoGeneraUnCasoDeExcesoDeCarga() {
        viaje = viaje(EstadoViaje.FINALIZADO, LocalDateTime.of(2026, 10, 1, 8, 0));
        pesajes.getPesajes().add(pesaje(SUR, 36500));

        motor.evaluarViaje(21L, Context.root());

        List<CasoFiscalizacionDto> exceso = casosDe(TipoInfraccion.EXCESO_CARGA);
        assertEquals(1, exceso.size());
        assertEquals("EXCESO_CARGA|11|viaje-21", exceso.get(0).getClaveDeduplicacion());
        assertEquals(21L, exceso.get(0).getIdViaje());
        EvidenciaDto evidencia = exceso.get(0).getEvidencias().get(0);
        assertEquals(TipoEvidencia.PESAJE, evidencia.getTipo());
        assertEquals(SUR, evidencia.getIdOrigen());
        assertEquals("36500 kg", evidencia.getValorObservado());
        assertEquals("33800 kg", evidencia.getValorDeclarado());
        assertEquals(1, casos.casos.size());
    }

    @Test
    void unPesajeIgualAlMaximoNoEsExceso() {
        viaje = viaje(EstadoViaje.FINALIZADO, LocalDateTime.of(2026, 10, 1, 8, 0));
        pesajes.getPesajes().add(pesaje(SUR, 33800));

        motor.evaluarViaje(21L, Context.root());

        assertTrue(casos.casos.isEmpty());
    }

    @Test
    void siUnaBalanzaNoRespondeQuedaPendienteHastaQueRespondanTodas() {
        viaje = viaje(EstadoViaje.FINALIZADO, LocalDateTime.of(2026, 10, 1, 8, 0));
        pesajes.getPesajes().add(pesaje(SUR, 36500));
        pesajes.getNodosSinResponder().add("balanzas-norte");

        motor.evaluarViaje(21L, Context.root());

        assertEquals(1, casosDe(TipoInfraccion.EXCESO_CARGA).size());
        assertEquals(AHORA.plusMinutes(1), proximaEvaluacion);

        pesajes.getNodosSinResponder().clear();
        motor.evaluarViaje(21L, Context.root());

        assertNull(proximaEvaluacion);
    }

    @Test
    void loQueLlegaDespuesSeAgregaAlMismoCasoSinDuplicar() {
        viaje = viaje(EstadoViaje.FINALIZADO, LocalDateTime.of(2026, 10, 1, 8, 0));
        pesajes.getPesajes().add(pesaje(SUR, 36500));
        pesajes.getNodosSinResponder().add("balanzas-norte");
        motor.evaluarViaje(21L, Context.root());

        pesajes.getNodosSinResponder().clear();
        pesajes.getPesajes().add(pesaje(NORTE, 35900));
        motor.evaluarViaje(21L, Context.root());

        List<CasoFiscalizacionDto> exceso = casosDe(TipoInfraccion.EXCESO_CARGA);
        assertEquals(1, exceso.size());
        assertEquals(List.of(SUR, NORTE), exceso.get(0).getEvidencias().stream().map(EvidenciaDto::getIdOrigen).toList());
    }

    @Test
    void elTemporizadorEvaluaLosViajesReservados() {
        viaje = viaje(EstadoViaje.FINALIZADO, LocalDateTime.of(2026, 10, 1, 8, 0));
        pesajes.getPesajes().add(pesaje(SUR, 36500));
        reservados = List.of(21L);

        motor.reevaluarCargasPendientes();

        assertEquals(AHORA.plusMinutes(1), reservadoHasta);
        List<CasoFiscalizacionDto> exceso = casosDe(TipoInfraccion.EXCESO_CARGA);
        assertEquals(1, exceso.size());
        assertEquals(21L, exceso.get(0).getIdViaje());
    }
}
