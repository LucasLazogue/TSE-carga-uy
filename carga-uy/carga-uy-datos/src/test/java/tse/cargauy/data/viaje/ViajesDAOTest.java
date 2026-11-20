package tse.cargauy.data.viaje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.data.BaseDatos;
import tse.cargauy.data.empresa.EmpresaDAO;
import tse.cargauy.data.eventoviaje.EventoViajeDAO;
import tse.cargauy.data.guia.GuiaDAO;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.FiltroGuias;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.ResultadoEvento;
import tse.cargauy.entities.TipoEventoViaje;

// Guias, viajes y eventos de viaje. Cada test arma su Guia y su viaje con un vehiculo propio, para no depender de los
// viajes de la demo; el chofer es Carlos (55555555), chofer de Transportes del Sur desde 2024
class ViajesDAOTest extends BaseDatos {

    private static final Paginacion TODO = new Paginacion(0, 100);
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 10);

    private GuiaDAO guias;
    private ViajeDAO viajes;
    private Long sur;
    private Long carlos;
    private Long vehiculo;
    private Long rubro;
    private Long tipoCarga;

    @BeforeEach
    void preparar() {
        guias = bean(GuiaDAO.class);
        viajes = bean(ViajeDAO.class);
        sur = bean(EmpresaDAO.class).getEmpresaByNro(1001).getId();
        carlos = bean(UsuarioDAO.class).getCiudadanoByCedula("55555555").getId();
        bean(VehiculoDAO.class).addVehiculo(new VehiculoDto("SVJ0001", "Volvo", "FH", 8000, 30000, sur));
        vehiculo = bean(VehiculoDAO.class).getVehiculoByMatricula("SVJ0001").getId();
        rubro = guias.getRubros().get(0).getId();
        tipoCarga = guias.getTiposCarga().get(0).getId();
    }

    private GuiaDto guia(LocalDate fecha) {
        return guias.addGuia(new GuiaDto(fecha, -34.90, -56.16, -30.90, -55.55, 15000, sur, carlos, rubro, tipoCarga));
    }

    private ViajeDto viaje(LocalDate fecha) {
        return viajes.addViaje(new ViajeDto(guia(fecha).getId(), vehiculo, carlos));
    }

    @Test
    void guiaPorIdModificarYBorrar() {
        GuiaDto creada = guia(FECHA);
        GuiaDto cambios = new GuiaDto(FECHA.plusDays(1), -34.0, -56.0, -31.0, -55.0, 9000, sur, carlos, rubro, tipoCarga);

        guias.updateGuia(creada.getId(), cambios);
        guias.updateGuia(-1L, cambios);
        sincronizar();
        GuiaDto modificada = guias.getGuiaById(creada.getId());
        assertEquals(FECHA.plusDays(1), modificada.getFecha());
        assertEquals(9000, modificada.getVolumen());
        assertEquals(-31.0, modificada.getDestinoLat());

        guias.deleteGuia(creada.getId());
        guias.deleteGuia(-1L);
        sincronizar();
        assertNull(guias.getGuiaById(creada.getId()));
    }

    @Test
    void catalogos() {
        assertEquals(guias.getRubros().get(0).getNombre(), guias.getRubroById(rubro).getNombre());
        assertNotNull(guias.getTipoCargaById(tipoCarga));
        assertNull(guias.getRubroById(-1L));
        assertNull(guias.getTipoCargaById(-1L));
    }

    @Test
    void buscarGuias() {
        GuiaDto sinViaje = guia(LocalDate.of(2031, 3, 1));
        GuiaDto conViaje = guia(LocalDate.of(2031, 3, 5));
        viajes.addViaje(new ViajeDto(conViaje.getId(), vehiculo, carlos));
        sincronizar();

        FiltroGuias marzo = new FiltroGuias(null, null, LocalDate.of(2031, 3, 1), LocalDate.of(2031, 3, 31));
        marzo.setIdsEmpresa(List.of(sur));
        assertEquals(2, guias.buscar(marzo, TODO).getTotal());

        FiltroGuias sinAsignar = new FiltroGuias(null, false, LocalDate.of(2031, 3, 1), null);
        assertEquals(List.of(sinViaje.getId()), guias.buscar(sinAsignar, TODO).getItems().stream().map(GuiaDto::getId).toList());
        FiltroGuias asignadas = new FiltroGuias(null, true, LocalDate.of(2031, 3, 1), null);
        assertEquals(List.of(conViaje.getId()), guias.buscar(asignadas, TODO).getItems().stream().map(GuiaDto::getId).toList());

        // la busqueda es por numero de Guia; un texto sin numero no encuentra nada
        assertEquals(1, guias.buscar(new FiltroGuias("G-" + sinViaje.getId(), null, null, null), TODO).getTotal());
        assertEquals(0, guias.buscar(new FiltroGuias("sin numero", null, null, null), TODO).getTotal());

        FiltroGuias ningunaEmpresa = new FiltroGuias();
        ningunaEmpresa.setIdsEmpresa(List.of());
        assertEquals(0, guias.buscar(ningunaEmpresa, TODO).getTotal());
    }

    @Test
    void viajePorGuiaReasignarYBorrar() {
        ViajeDto creado = viaje(FECHA);
        Long sta = bean(VehiculoDAO.class).getVehiculoByMatricula("STA1234").getId();

        assertEquals(creado.getId(), viajes.getByGuia(creado.getIdGuia()).getId());
        viajes.updateViaje(creado.getId(), new ViajeDto(creado.getIdGuia(), sta, carlos));
        viajes.updateViaje(-1L, creado);
        sincronizar();
        assertEquals(sta, viajes.getViajeById(creado.getId()).getIdVehiculo());

        viajes.deleteViaje(creado.getId());
        viajes.deleteViaje(-1L);
        sincronizar();
        assertNull(viajes.getViajeById(creado.getId()));
        assertNull(viajes.getByGuia(creado.getIdGuia()), "la Guia queda libre para otro viaje");
        assertNotNull(guias.getGuiaById(creado.getIdGuia()));
    }

    // primero el que esta en curso, despues los asignados por fecha de la Guia
    @Test
    void viajesActivosDelChofer() {
        ViajeDto lejano = viaje(LocalDate.of(2031, 5, 20));
        ViajeDto enCurso = viaje(LocalDate.of(2031, 5, 25));
        viajes.cambiarEstado(enCurso.getId(), EstadoViaje.EN_CURSO, LocalDateTime.of(2031, 5, 25, 8, 0));
        sincronizar();

        List<Long> activos = viajes.getActivosDelChofer(carlos).stream().map(ViajeDto::getId).toList();
        assertEquals(enCurso.getId(), activos.get(0));
        assertTrue(activos.contains(lejano.getId()));

        Long vinculo = bean(UsuarioDAO.class).getChoferVigente(carlos, sur, FECHA);
        List<Long> pendientes = viajes.getPendientesDelVinculo(vinculo).stream().map(ViajeDto::getId).toList();
        assertTrue(pendientes.containsAll(List.of(lejano.getId(), enCurso.getId())));
    }

    @Test
    void cicloDeEstadosYCorreccionDelInicio() {
        ViajeDto creado = viaje(FECHA);
        LocalDateTime inicio = FECHA.atTime(8, 0);

        viajes.cambiarEstado(creado.getId(), EstadoViaje.EN_CURSO, inicio);
        viajes.corregirInicio(creado.getId(), inicio.minusMinutes(30));
        viajes.corregirInicio(-1L, inicio);
        viajes.cambiarEstado(creado.getId(), EstadoViaje.FINALIZADO, FECHA.atTime(18, 0));
        viajes.cambiarEstado(-1L, EstadoViaje.FINALIZADO, inicio);
        sincronizar();

        ViajeDto terminado = viajes.getViajeParaActualizar(creado.getId());
        assertEquals(EstadoViaje.FINALIZADO, terminado.getEstado());
        assertEquals(inicio.minusMinutes(30), terminado.getFechaInicio());
        assertEquals(FECHA.atTime(18, 0), terminado.getFechaFin());
        assertNull(viajes.getViajeParaActualizar(-1L));
    }

    // un chofer o un vehiculo no pueden estar en dos viajes a la vez: se compara contra los otros viajes iniciados
    @Test
    void otroViajeEnCursoEnElMismoHorario() {
        ViajeDto primero = viaje(LocalDate.of(2031, 7, 1));
        ViajeDto segundo = viaje(LocalDate.of(2031, 7, 1));
        viajes.cambiarEstado(primero.getId(), EstadoViaje.EN_CURSO, LocalDateTime.of(2031, 7, 1, 8, 0));
        viajes.cambiarEstado(primero.getId(), EstadoViaje.FINALIZADO, LocalDateTime.of(2031, 7, 1, 12, 0));
        sincronizar();

        LocalDateTime medioDia = LocalDateTime.of(2031, 7, 1, 10, 0);
        LocalDateTime tarde = LocalDateTime.of(2031, 7, 1, 13, 0);
        assertTrue(viajes.choferConOtroEnCurso(segundo.getId(), carlos, medioDia, null));
        assertTrue(viajes.vehiculoConOtroEnCurso(segundo.getId(), vehiculo, medioDia, tarde));
        assertFalse(viajes.vehiculoConOtroEnCurso(segundo.getId(), vehiculo, tarde, null), "el primero ya habia terminado");
        assertFalse(viajes.vehiculoConOtroEnCurso(segundo.getId(), vehiculo, LocalDateTime.of(2031, 7, 1, 6, 0),
                LocalDateTime.of(2031, 7, 1, 7, 0)), "todavia no habia empezado");
        assertFalse(viajes.vehiculoConOtroEnCurso(primero.getId(), vehiculo, medioDia, null), "no se compara consigo mismo");
    }

    // el viaje del vehiculo en un momento: sirve para asociar las posiciones y los pesajes al viaje en curso
    @Test
    void viajeEnCursoDelVehiculo() {
        ViajeDto creado = viaje(LocalDate.of(2031, 8, 1));
        viajes.cambiarEstado(creado.getId(), EstadoViaje.EN_CURSO, LocalDateTime.of(2031, 8, 1, 8, 0));
        sincronizar();

        assertEquals(creado.getId(), viajes.getEnCursoDelVehiculo(vehiculo, LocalDateTime.of(2031, 8, 1, 9, 0)).getId());
        assertNull(viajes.getEnCursoDelVehiculo(vehiculo, LocalDateTime.of(2031, 8, 1, 7, 0)));
        assertEquals(List.of(creado.getId()), viajes.getIniciadosDelVehiculo(vehiculo, LocalDateTime.of(2031, 8, 1, 0, 0))
                .stream().map(ViajeDto::getId).toList());
    }

    // la evaluacion de exceso de carga se reserva con UPDATE ... FOR UPDATE SKIP LOCKED ... RETURNING (SQL de PostgreSQL)
    @Test
    void reservaDeEvaluacionesDeCarga() {
        ViajeDto vencido = viaje(FECHA);
        ViajeDto futuro = viaje(FECHA);
        LocalDateTime ahora = LocalDateTime.of(2031, 1, 1, 12, 0);
        viajes.marcarEvaluacionCarga(vencido.getId(), ahora.minusMinutes(1));
        viajes.marcarEvaluacionCarga(futuro.getId(), ahora.plusHours(1));

        List<Long> reservados = viajes.reservarEvaluacionesCarga(ahora, ahora.plusMinutes(10), 100);

        assertTrue(reservados.contains(vencido.getId()));
        assertFalse(reservados.contains(futuro.getId()));
        assertFalse(viajes.reservarEvaluacionesCarga(ahora, ahora.plusMinutes(10), 100).contains(vencido.getId()),
                "reservado, no lo vuelve a tomar hasta que venza la marca");
    }

    // los eventos del movil quedan registrados con su resultado; el mismo idEvento reenviado se reconoce (idempotencia)
    @Test
    void eventosDelViaje() {
        EventoViajeDao eventos = new EventoViajeDao(bean(EventoViajeDAO.class), carlos);
        ViajeDto creado = viaje(FECHA);

        eventos.registrar("ev-1", creado.getId(), TipoEventoViaje.INICIO, Instant.parse("2026-11-10T11:00:00Z"), ResultadoEvento.APLICADO);
        eventos.registrar("ev-2", creado.getId(), TipoEventoViaje.INCIDENTE, Instant.parse("2026-11-10T12:00:00Z"), ResultadoEvento.CONFLICTO);
        sincronizar();

        assertTrue(eventos.dao().existe("ev-1"));
        assertFalse(eventos.dao().existe("ev-99"));
        PaginaDto<EventoViajeDto> delViaje = eventos.dao().getByViaje(creado.getId(), TODO);
        assertEquals(2, delViaje.getTotal());
        assertEquals("ev-1", delViaje.getItems().get(0).getIdEvento(), "en el orden en que se generaron");
        assertEquals("55555555", delViaje.getItems().get(0).getCedulaChofer());
        assertEquals(ResultadoEvento.CONFLICTO, delViaje.getItems().get(1).getResultado());
    }

    private record EventoViajeDao(EventoViajeDAO dao, Long idChofer) {
        void registrar(String id, Long idViaje, TipoEventoViaje tipo, Instant generado, ResultadoEvento resultado) {
            EventoViajeDto evento = new EventoViajeDto();
            evento.setIdEvento(id);
            evento.setIdViaje(idViaje);
            evento.setTipo(tipo);
            evento.setTimestampGeneracion(generado);
            dao.registrar(evento, idChofer, resultado, resultado == ResultadoEvento.CONFLICTO ? "otro viaje en curso" : null);
        }
    }
}
