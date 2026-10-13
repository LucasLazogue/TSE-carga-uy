package tse.cargauy.nodotracking;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

// arma los vehiculos simulados: los de un archivo de rutas y, para la prueba de carga, otros generados al azar
final class Flota {

    private static final String RUTAS_INCLUIDAS = "/rutas-prueba.json";

    private static final List<Punto> CIUDADES = List.of(
            new Punto(-34.9011, -56.1645), // Montevideo
            new Punto(-31.3833, -57.9667), // Salto
            new Punto(-32.3214, -58.0756), // Paysandu
            new Punto(-30.9053, -55.5508), // Rivera
            new Punto(-31.7110, -55.9811), // Tacuarembo
            new Punto(-32.3703, -54.1675), // Melo
            new Punto(-34.4626, -57.8400), // Colonia
            new Punto(-33.1325, -58.2956), // Fray Bentos
            new Punto(-34.9000, -54.9500), // Maldonado
            new Punto(-33.3806, -56.5236)); // Durazno

    private Flota() {
    }

    static List<VehiculoSimulado> armar(ConfigFlota config, Random azar) throws IOException {
        List<VehiculoSimulado> flota = new ArrayList<>(cargar(config.rutas()));
        flota.addAll(generar(config.vehiculosExtra(), azar));
        return flota;
    }

    // formato: [{"matricula":"STA1234","origen":{"lat":..,"lon":..},"destino":{...},"velocidadKmh":80,"desvioKm":0}]
    static List<VehiculoSimulado> cargar(String archivo) throws IOException {
        try (InputStream entrada = archivo == null
                ? Flota.class.getResourceAsStream(RUTAS_INCLUIDAS)
                : Files.newInputStream(Path.of(archivo));
                JsonReader lector = Json.createReader(entrada)) {
            List<VehiculoSimulado> flota = new ArrayList<>();
            for (JsonValue valor : lector.readArray()) {
                JsonObject json = valor.asJsonObject();
                flota.add(new VehiculoSimulado(
                        json.getString("matricula"),
                        new Ruta(punto(json.getJsonObject("origen")), punto(json.getJsonObject("destino"))),
                        json.getJsonNumber("velocidadKmh").doubleValue(),
                        json.containsKey("desvioKm") ? json.getJsonNumber("desvioKm").doubleValue() : 0));
            }
            return flota;
        }
    }

    // matriculas SIM0001, SIM0002...: no estan registradas en el central salvo que se carguen aparte
    static List<VehiculoSimulado> generar(int cantidad, Random azar) {
        List<VehiculoSimulado> flota = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            Punto origen = CIUDADES.get(azar.nextInt(CIUDADES.size()));
            Punto destino = origen;
            while (destino.equals(origen)) {
                destino = CIUDADES.get(azar.nextInt(CIUDADES.size()));
            }
            flota.add(new VehiculoSimulado(String.format("SIM%04d", i), new Ruta(origen, destino), 60 + azar.nextInt(31), 0));
        }
        return flota;
    }

    private static Punto punto(JsonObject json) {
        return new Punto(json.getJsonNumber("lat").doubleValue(), json.getJsonNumber("lon").doubleValue());
    }
}
