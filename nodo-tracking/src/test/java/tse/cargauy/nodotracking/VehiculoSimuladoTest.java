package tse.cargauy.nodotracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class VehiculoSimuladoTest {

    private static final Punto MONTEVIDEO = new Punto(-34.9011, -56.1645);
    private static final Punto RIVERA = new Punto(-30.9053, -55.5508);
    private static final Instant AHORA = Instant.parse("2026-10-08T13:00:00Z");
    private static final Duration UN_MINUTO = Duration.ofMinutes(1);

    @Test
    void cadaTickTieneSuPropioIdYLaHoraDada() {
        VehiculoSimulado vehiculo = new VehiculoSimulado("STA1234", new Ruta(MONTEVIDEO, RIVERA), 60, 0);

        EventoGps primero = vehiculo.avanzar(AHORA, UN_MINUTO, 1);
        EventoGps segundo = vehiculo.avanzar(AHORA.plusSeconds(60), UN_MINUTO, 1);

        assertNotEquals(primero.idEvento(), segundo.idEvento());
        assertEquals("STA1234", primero.matricula());
        assertEquals(AHORA, primero.timestampGeneracion());
    }

    @Test
    void avanzaLaDistanciaQueCorrespondeALaVelocidad() {
        VehiculoSimulado vehiculo = new VehiculoSimulado("STA1234", new Ruta(MONTEVIDEO, RIVERA), 60, 0);

        // 60 km/h durante un minuto = 1 km
        EventoGps evento = vehiculo.avanzar(AHORA, UN_MINUTO, 1);

        assertEquals(1, Geo.distanciaKm(MONTEVIDEO, new Punto(evento.latitud(), evento.longitud())), 0.01);
    }

    @Test
    void acelerarMultiplicaLaDistanciaPeroNoLaHora() {
        VehiculoSimulado vehiculo = new VehiculoSimulado("STA1234", new Ruta(MONTEVIDEO, RIVERA), 60, 0);

        EventoGps evento = vehiculo.avanzar(AHORA, UN_MINUTO, 30);

        assertEquals(30, Geo.distanciaKm(MONTEVIDEO, new Punto(evento.latitud(), evento.longitud())), 0.1);
        assertEquals(AHORA, evento.timestampGeneracion());
    }

    @Test
    void alLlegarEmprendeLaVuelta() {
        // 448 km a 600 km/h acelerado: llega en el primer tick de una hora
        VehiculoSimulado vehiculo = new VehiculoSimulado("STA1234", new Ruta(MONTEVIDEO, RIVERA), 600, 0);

        EventoGps enRivera = vehiculo.avanzar(AHORA, Duration.ofHours(1), 1);
        EventoGps deVuelta = vehiculo.avanzar(AHORA, UN_MINUTO, 1);

        assertEquals(0, Geo.distanciaKm(RIVERA, new Punto(enRivera.latitud(), enRivera.longitud())), 0.01);
        assertEquals(10, Geo.distanciaKm(RIVERA, new Punto(deVuelta.latitud(), deVuelta.longitud())), 0.1);
    }

    @Test
    void elDesvioSoloSeAplicaEnElTercioCentral() {
        Ruta ruta = new Ruta(MONTEVIDEO, RIVERA);
        VehiculoSimulado conDesvio = new VehiculoSimulado("STA1234", ruta, 60, 15);

        // a 60 km/h, 10 minutos = 10 km: todavia en el primer tercio
        EventoGps alPrincipio = conDesvio.avanzar(AHORA, Duration.ofMinutes(10), 1);
        // 3 horas mas = 190 km: en el tercio central (entre 149 y 299 km)
        EventoGps enElMedio = conDesvio.avanzar(AHORA, Duration.ofHours(3), 1);

        Punto sobreLaRuta = ruta.puntoEn(190, 0);
        assertEquals(0, Geo.distanciaKm(ruta.puntoEn(10, 0), new Punto(alPrincipio.latitud(), alPrincipio.longitud())), 0.01);
        assertTrue(Geo.distanciaKm(sobreLaRuta, new Punto(enElMedio.latitud(), enElMedio.longitud())) > 14);
    }
}
