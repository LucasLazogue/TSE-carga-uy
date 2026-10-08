package tse.cargauy.nodotracking;

import java.time.Duration;

// como se comporta la flota simulada; cada escenario de prueba se arma cambiando estas variables
public record ConfigFlota(
        String rutas,               // archivo de rutas; null = rutas-prueba.json incluido en el jar
        int vehiculosExtra,         // vehiculos SIM0001... con rutas al azar, para la prueba de carga
        Duration intervalo,         // cada cuanto reporta cada vehiculo
        double acelerar,            // multiplica la distancia de cada tick, para no esperar horas un viaje
        double duplicados,          // fraccion de eventos que se publican dos veces (idempotencia)
        boolean desordenar,         // mezcla cada lote antes de publicarlo (orden por timestampGeneracion)
        Duration sinCoberturaDesde, // null = siempre con cobertura
        Duration sinCoberturaDurante) {

    public static ConfigFlota desdeEntorno() {
        String rutas = Config.texto("NODO_RUTAS", null);
        Duration desde = null;
        Duration durante = null;
        // formato "desde,durante" en minutos, por ejemplo "10,5"
        String sinCobertura = Config.texto("NODO_SIN_COBERTURA", null);
        if (sinCobertura != null) {
            String[] partes = sinCobertura.split(",");
            if (partes.length != 2) {
                throw new IllegalStateException("NODO_SIN_COBERTURA debe tener el formato desde,durante (en minutos)");
            }
            desde = Duration.ofMinutes(Long.parseLong(partes[0].trim()));
            durante = Duration.ofMinutes(Long.parseLong(partes[1].trim()));
        }
        return new ConfigFlota(
                rutas,
                Config.numero("NODO_VEHICULOS_EXTRA", 0),
                Duration.ofSeconds(Config.numero("NODO_INTERVALO_SEG", 10)),
                Config.decimal("NODO_ACELERAR", 1),
                Config.decimal("NODO_DUPLICADOS", 0),
                Boolean.parseBoolean(Config.texto("NODO_DESORDENAR", "false")),
                desde,
                durante);
    }
}
