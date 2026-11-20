package tse.cargauy.nodotracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// la flota sale de un archivo de rutas (o del incluido en el jar) mas los vehiculos generados para la prueba de carga
class FlotaArmadoTest {

    @Test
    void lasRutasIncluidasMasLosGenerados() throws IOException {
        ConfigFlota config = new ConfigFlota(null, 2, Duration.ofSeconds(30), 1, 0, false, null, null, 50);

        List<VehiculoSimulado> flota = Flota.armar(config, new Random(1));

        List<VehiculoSimulado> incluidas = Flota.cargar(null);
        assertFalse(incluidas.isEmpty());
        assertEquals(incluidas.size() + 2, flota.size());
        assertEquals("SIM0050", flota.get(incluidas.size()).getMatricula());
        assertEquals("SIM0051", flota.get(flota.size() - 1).getMatricula());
    }

    @Test
    void unArchivoDeRutasPropio(@TempDir Path carpeta) throws IOException {
        Path archivo = carpeta.resolve("rutas.json");
        Files.writeString(archivo, """
                [{"matricula":"STA1234","origen":{"lat":-34.9,"lon":-56.16},"destino":{"lat":-30.9,"lon":-55.55},"velocidadKmh":80},
                 {"matricula":"SBB5678","origen":{"lat":-34.9,"lon":-56.16},"destino":{"lat":-33.1,"lon":-58.3},"velocidadKmh":70,"desvioKm":8}]
                """);

        List<VehiculoSimulado> flota = Flota.cargar(archivo.toString());

        assertEquals(2, flota.size());
        assertEquals("STA1234", flota.get(0).getMatricula());
        assertEquals(0, flota.get(0).getDesvioKm(), "sin desvioKm va sobre la ruta");
        assertEquals(8, flota.get(1).getDesvioKm());
    }
}
