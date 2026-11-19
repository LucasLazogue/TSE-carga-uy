package tse.cargauy.nodotracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class FlotaTest {

    @Test
    void numeraLosVehiculosSimuladosDesdeElPrimero() {
        List<String> matriculas = Flota.generar(3, 9998, new Random(1)).stream()
                .map(VehiculoSimulado::getMatricula)
                .toList();

        assertEquals(List.of("SIM9998", "SIM9999", "SIM10000"), matriculas);
    }

    @Test
    void sinIndicarElPrimeroArrancaEnUno() {
        ConfigFlota config = new ConfigFlota(null, 2, java.time.Duration.ofSeconds(30), 1, 0, false, null, null);

        assertEquals(1, config.primerVehiculo());
        assertEquals("SIM0001", Flota.generar(2, config.primerVehiculo(), new Random(1)).get(0).getMatricula());
    }

    @Test
    void laRutaSaleDelNumeroComoEnElJuegoDeDatos() {
        // SIM0001: Salto -> Paysandu; SIM0010: Montevideo -> Paysandu; SIM0123: Rivera -> Fray Bentos
        assertEquals(new Ruta(new Punto(-31.3833, -57.9667), new Punto(-32.3214, -58.0756)), Flota.ruta(1));
        assertEquals(new Ruta(new Punto(-34.9011, -56.1645), new Punto(-32.3214, -58.0756)), Flota.ruta(10));
        assertEquals(new Ruta(new Punto(-30.9053, -55.5508), new Punto(-33.1325, -58.2956)), Flota.ruta(123));
        for (int n = 1; n <= 18000; n++) {
            assertNotEquals(Flota.ruta(n).origen(), Flota.ruta(n).destino(), "SIM" + n);
        }
    }

    @Test
    void unoDeCadaCienSeDesvia() {
        List<VehiculoSimulado> flota = Flota.generar(200, 1, new Random(1));

        assertEquals(2, flota.stream().filter(v -> v.getDesvioKm() > 0).count());
    }
}

