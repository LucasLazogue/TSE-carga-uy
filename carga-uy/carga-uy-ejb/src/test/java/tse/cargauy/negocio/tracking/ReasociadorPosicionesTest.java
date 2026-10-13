package tse.cargauy.negocio.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import tse.cargauy.data.posicion.EventoPosicionDAO;
import tse.cargauy.data.viaje.ViajeDAO;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.negocio.tracking.ReasociadorPosiciones.Intervalo;
import tse.cargauy.negocio.tracking.ReasociadorPosiciones.Tramo;

class ReasociadorPosicionesTest {

    private static final ZoneId ZONA = ZoneId.systemDefault();

    // el viaje sigue en curso: todo lo generado desde su inicio es suyo
    @Test
    void unViajeEnCursoSeQuedaConTodoDesdeSuInicio() {
        assertEquals(List.of(new Tramo(h(10), null, 1L)),
                ReasociadorPosiciones.tramos(List.of(new Intervalo(1L, h(10), null)), h(10)));
    }

    // el fin llego tarde: lo generado despues del fin vuelve a quedar sin viaje
    @Test
    void loGeneradoDespuesDelFinQuedaSinViaje() {
        assertEquals(List.of(new Tramo(h(10), h(12), 1L), new Tramo(h(12), null, null)),
                ReasociadorPosiciones.tramos(List.of(new Intervalo(1L, h(10), h(12))), h(10)));
    }

    @Test
    void entreDosViajesHayUnTramoSinViaje() {
        List<Intervalo> viajes = List.of(new Intervalo(1L, h(8), h(10)), new Intervalo(2L, h(12), null));

        assertEquals(List.of(new Tramo(h(8), h(10), 1L), new Tramo(h(10), h(12), null), new Tramo(h(12), null, 2L)),
                ReasociadorPosiciones.tramos(viajes, h(8)));
    }

    // el primer viaje quedo sin fin registrado cuando empezo el segundo: desde ese momento las posiciones son del segundo
    @Test
    void siDosViajesSeSuperponenGanaElQueEmpezoUltimo() {
        List<Intervalo> viajes = List.of(new Intervalo(1L, h(8), null), new Intervalo(2L, h(12), null));

        assertEquals(List.of(new Tramo(h(8), h(12), 1L), new Tramo(h(12), null, 2L)),
                ReasociadorPosiciones.tramos(viajes, h(8)));
    }

    // el fin del primero llega despues del inicio del segundo
    @Test
    void unFinTardioDelViajeAnteriorNoLeQuitaPosicionesAlSiguiente() {
        List<Intervalo> viajes = List.of(new Intervalo(1L, h(8), h(10)), new Intervalo(2L, h(9), null));

        assertEquals(List.of(new Tramo(h(8), h(9), 1L), new Tramo(h(9), null, 2L)),
                ReasociadorPosiciones.tramos(viajes, h(8)));
    }

    // un viaje que empezo antes de 'desde' y sigue en curso se queda con lo que va desde 'desde'
    @Test
    void empiezaEnDesdeAunqueHayaUnViajeQueVieneDeAntes() {
        List<Intervalo> viajes = List.of(new Intervalo(1L, h(6), null), new Intervalo(2L, h(10), h(11)));

        assertEquals(List.of(new Tramo(h(10), h(11), 2L), new Tramo(h(11), null, 1L)),
                ReasociadorPosiciones.tramos(viajes, h(10)));
    }

    // MongoDB guarda milisegundos: una posicion del mismo milisegundo del fin todavia es del viaje
    @Test
    void elFinIncluyeSuMilisegundo() {
        ViajeDto viaje = viaje(1L, local(10), LocalDateTime.ofInstant(h(12), ZONA).plusNanos(400_000));

        Intervalo intervalo = Intervalo.de(viaje);

        assertTrue(intervalo.cubre(h(12)));
        assertEquals(h(12).plusMillis(1), intervalo.finExclusivo());
    }

    @Test
    void reasociaLasPosicionesDelVehiculoDesdeElInicioDelViaje() {
        ReasociadorPosiciones reasociador = new ReasociadorPosiciones();
        ViajeDto viaje = viaje(1L, local(10), local(12));
        List<Object[]> asociaciones = new ArrayList<>();
        List<LocalDateTime> desdes = new ArrayList<>();
        reasociador.viajeDAO = new ViajeDAO() {
            @Override
            public ViajeDto getViajeById(Long id) {
                return id.equals(1L) ? viaje : null;
            }

            @Override
            public List<ViajeDto> getIniciadosDelVehiculo(Long idVehiculo, LocalDateTime desde) {
                desdes.add(desde);
                return List.of(viaje);
            }
        };
        reasociador.eventoDAO = new EventoPosicionDAO() {
            @Override
            public void asociarAViaje(String matricula, Long idVehiculo, Instant desde, Instant hasta, Long idViaje) {
                asociaciones.add(new Object[] { matricula, idVehiculo, desde, hasta, idViaje });
            }
        };

        reasociador.reasociar(1L);

        assertEquals(List.of(local(10)), desdes);
        assertEquals(2, asociaciones.size());
        assertEquals(Arrays.asList("STA1234", 11L, h(10), h(12).plusMillis(1), 1L), Arrays.asList(asociaciones.get(0)));
        assertEquals(Arrays.asList("STA1234", 11L, h(12).plusMillis(1), null, null), Arrays.asList(asociaciones.get(1)));
    }

    @Test
    void unViajeSinInicioNoReasociaNada() {
        ReasociadorPosiciones reasociador = new ReasociadorPosiciones();
        List<Long> asociados = new ArrayList<>();
        reasociador.viajeDAO = new ViajeDAO() {
            @Override
            public ViajeDto getViajeById(Long id) {
                return viaje(1L, null, null);
            }
        };
        reasociador.eventoDAO = new EventoPosicionDAO() {
            @Override
            public void asociarAViaje(String matricula, Long idVehiculo, Instant desde, Instant hasta, Long idViaje) {
                asociados.add(idViaje);
            }
        };

        reasociador.reasociar(1L);

        assertTrue(asociados.isEmpty());
    }

    private static ViajeDto viaje(Long id, LocalDateTime inicio, LocalDateTime fin) {
        ViajeDto viaje = new ViajeDto();
        viaje.setId(id);
        viaje.setIdVehiculo(11L);
        viaje.setMatricula("STA1234");
        viaje.setFechaInicio(inicio);
        viaje.setFechaFin(fin);
        return viaje;
    }

    private static Instant h(int hora) {
        return Instant.parse("2026-10-06T00:00:00Z").plusSeconds(hora * 3600L);
    }

    private static LocalDateTime local(int hora) {
        return LocalDateTime.ofInstant(h(hora), ZONA);
    }
}
