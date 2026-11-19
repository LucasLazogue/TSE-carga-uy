package tse.cargauy.nodotracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
