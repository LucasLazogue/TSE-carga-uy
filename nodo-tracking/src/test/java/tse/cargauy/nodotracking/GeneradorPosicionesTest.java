package tse.cargauy.nodotracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

// con un publicador falso: prueba la logica de generar, acumular y reintentar sin necesitar el central
class GeneradorPosicionesTest {

    private static final Instant INICIO = Instant.parse("2026-10-08T13:00:00Z");
    private static final Ruta RUTA = new Ruta(new Punto(-34.9011, -56.1645), new Punto(-30.9053, -55.5508));

    private final PublicadorFalso publicador = new PublicadorFalso();

    @Test
    void publicaUnEventoPorVehiculoYTick() {
        GeneradorPosiciones generador = generador(flota(), 3, 500);

        generador.generar(INICIO);
        generador.generar(INICIO.plusSeconds(10));
        generador.publicarPendientes(INICIO.plusSeconds(11));

        assertEquals(6, publicador.recibidos.size());
        assertEquals(6, generador.generados.get());
        assertEquals(0, generador.cantidadPendiente());
    }

    @Test
    void publicaDeALotesDelTamanioMaximo() {
        GeneradorPosiciones generador = generador(flota(), 5, 2);

        generador.generar(INICIO);
        generador.publicarPendientes(INICIO.plusSeconds(1));

        assertEquals(List.of(2, 2, 1), publicador.tamaniosDeLote);
    }

    @Test
    void siElBrokerFallaNoPierdeNadaYReintentaDespues() {
        GeneradorPosiciones generador = generador(flota(), 2, 500);
        generador.generar(INICIO);
        publicador.fallar = true;

        generador.publicarPendientes(INICIO.plusSeconds(1));
        assertEquals(2, generador.cantidadPendiente());
        assertEquals(1, generador.reintentos.get());

        publicador.fallar = false;
        // todavia dentro de la espera: no intenta
        generador.publicarPendientes(INICIO.plusSeconds(1).plusMillis(500));
        assertEquals(0, publicador.recibidos.size());
        // pasada la espera maxima del primer reintento (1 s + 1 s al azar)
        generador.publicarPendientes(INICIO.plusSeconds(4));
        assertEquals(2, publicador.recibidos.size());
        assertEquals(0, generador.cantidadPendiente());
    }

    @Test
    void sinCoberturaAcumulaYAlVolverPublicaTodoJunto() {
        ConfigFlota flota = new ConfigFlota(null, 0, Duration.ofSeconds(10), 1, 0, false,
                Duration.ofMinutes(1), Duration.ofMinutes(1));
        GeneradorPosiciones generador = generador(flota, 2, 500);

        generador.generar(INICIO.plusSeconds(70));   // sin cobertura
        generador.generar(INICIO.plusSeconds(80));   // sin cobertura
        generador.publicarPendientes(INICIO.plusSeconds(81));
        assertEquals(0, publicador.recibidos.size());
        assertEquals(4, generador.cantidadRetenida());

        generador.generar(INICIO.plusSeconds(130));  // volvio la cobertura
        generador.publicarPendientes(INICIO.plusSeconds(131));

        assertEquals(6, publicador.recibidos.size());
        assertEquals(0, generador.cantidadRetenida());
    }

    @Test
    void losDuplicadosRepitenElMismoMensaje() {
        ConfigFlota flota = new ConfigFlota(null, 0, Duration.ofSeconds(10), 1, 1.0, false, null, null);
        GeneradorPosiciones generador = generador(flota, 3, 500);

        generador.generar(INICIO);
        generador.publicarPendientes(INICIO.plusSeconds(1));

        assertEquals(6, publicador.recibidos.size());
        assertEquals(3, new HashSet<>(publicador.recibidos).size());
        assertEquals(3, generador.duplicados.get());
    }

    @Test
    void desordenarMezclaElLoteSinPerderNinguno() {
        ConfigFlota flota = new ConfigFlota(null, 0, Duration.ofSeconds(10), 1, 0, true, null, null);
        GeneradorPosiciones generador = generador(flota, 20, 500);

        generador.generar(INICIO);
        generador.publicarPendientes(INICIO.plusSeconds(1));

        assertEquals(20, new HashSet<>(publicador.recibidos).size());
        assertTrue(publicador.tamaniosDeLote.size() == 1);
    }

    private GeneradorPosiciones generador(ConfigFlota flota, int vehiculos, int maxLote) {
        Config config = new Config("nodo-test", 0, "no-se-usa", "u", "c", maxLote);
        List<VehiculoSimulado> lista = new ArrayList<>();
        for (int i = 0; i < vehiculos; i++) {
            lista.add(new VehiculoSimulado("V" + i, RUTA, 80, 0));
        }
        return new GeneradorPosiciones(config, flota, lista, publicador, new Random(1), INICIO);
    }

    private static ConfigFlota flota() {
        return new ConfigFlota(null, 0, Duration.ofSeconds(10), 1, 0, false, null, null);
    }

    static class PublicadorFalso implements Publicador {
        final List<String> recibidos = new ArrayList<>();
        final List<Integer> tamaniosDeLote = new ArrayList<>();
        boolean fallar;

        @Override
        public void publicar(List<String> mensajes) throws PublicadorException {
            if (fallar) {
                throw new PublicadorException("falla simulada", null);
            }
            tamaniosDeLote.add(mensajes.size());
            recibidos.addAll(mensajes);
        }

        @Override
        public boolean disponible() {
            return !fallar;
        }
    }
}
