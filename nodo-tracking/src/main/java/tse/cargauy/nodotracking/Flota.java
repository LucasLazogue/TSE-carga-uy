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
        flota.addAll(generar(config.vehiculosExtra(), config.primerVehiculo(), azar));
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

    // uno de cada CADA_DESVIO vehiculos simulados se aparta DESVIO_KM de su ruta en el tercio central, por encima de la
    // distancia de desvio de la norma (5 km): la regla de desvio de ruta se ejercita en la prueba sin que la mayoria
    // de los viajes la dispare
    static final int CADA_DESVIO = 100;
    static final double DESVIO_KM = 8;

    // matriculas SIM0001, SIM0002... desde 'primero': no estan registradas en el central salvo que se carguen aparte
    // (performance/datos.sql registra SIM0001 a SIM18000). La ruta sale del numero, no del azar, para que la Guia del
    // viaje que registra performance/datos.sql tenga el mismo origen y destino: si no, casi todas las posiciones
    // quedarian fuera del tramo de la Guia y abririan casos de desvio que no son
    static List<VehiculoSimulado> generar(int cantidad, int primero, Random azar) {
        List<VehiculoSimulado> flota = new ArrayList<>();
        for (int i = primero; i < primero + cantidad; i++) {
            double desvio = i % CADA_DESVIO == 0 ? DESVIO_KM : 0;
            flota.add(new VehiculoSimulado(String.format("SIM%04d", i), ruta(i), 60 + azar.nextInt(31), desvio));
        }
        return flota;
    }

    // la misma cuenta que performance/datos.sql: origen CIUDADES[n % 10] y destino a 1 a 9 lugares, nunca el mismo
    static Ruta ruta(int n) {
        Punto origen = CIUDADES.get(n % CIUDADES.size());
        Punto destino = CIUDADES.get((n + 1 + (n / CIUDADES.size()) % (CIUDADES.size() - 1)) % CIUDADES.size());
        return new Ruta(origen, destino);
    }

    private static Punto punto(JsonObject json) {
        return new Punto(json.getJsonNumber("lat").doubleValue(), json.getJsonNumber("lon").doubleValue());
    }
}
