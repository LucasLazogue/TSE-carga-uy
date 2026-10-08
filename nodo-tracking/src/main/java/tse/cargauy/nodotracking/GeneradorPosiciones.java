package tse.cargauy.nodotracking;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;

// el sistema de tracking simulado: genera la posicion de cada vehiculo y la publica en la cola del central.
// generar y publicar van separados: si el central no responde, lo generado se acumula y se publica despues,
// sin perder nada
public class GeneradorPosiciones {

    private static final Logger LOGGER = Logger.getLogger(GeneradorPosiciones.class.getName());
    private static final long ESPERA_MAXIMA_MS = 30_000;

    private final Config config;
    private final ConfigFlota flota;
    private final List<VehiculoSimulado> vehiculos;
    private final Publicador publicador;
    private final Random azar;
    private final Instant inicio;

    // mensajes listos para publicar, en orden de generacion. vive en memoria: si el central cae por horas con
    // muchos vehiculos crece mucho, pero un GPS real tambien guarda lo que no pudo mandar
    private final Deque<String> pendientes = new ArrayDeque<>();
    // generados mientras el vehiculo no tiene cobertura: se liberan todos juntos al volver (el pico de 3x de la letra)
    private final List<String> retenidos = new ArrayList<>();

    // solo los usa el hilo que publica
    private Instant proximoIntento = Instant.EPOCH;
    private int intentosFallidos;

    final AtomicLong generados = new AtomicLong();
    final AtomicLong duplicados = new AtomicLong();
    final AtomicLong publicados = new AtomicLong();
    final AtomicLong reintentos = new AtomicLong();

    public GeneradorPosiciones(Config config, ConfigFlota flota, List<VehiculoSimulado> vehiculos,
            Publicador publicador, Random azar, Instant inicio) {
        this.config = config;
        this.flota = flota;
        this.vehiculos = vehiculos;
        this.publicador = publicador;
        this.azar = azar;
        this.inicio = inicio;
    }

    // lo llama el reloj cada intervalo: un evento por vehiculo
    public synchronized void generar(Instant ahora) {
        boolean sinCobertura = sinCobertura(ahora);
        if (!sinCobertura && !retenidos.isEmpty()) {
            LOGGER.info("nodo cobertura=RECUPERADA eventos_acumulados=" + retenidos.size());
            pendientes.addAll(retenidos);
            retenidos.clear();
        }
        for (VehiculoSimulado vehiculo : vehiculos) {
            String mensaje = vehiculo.avanzar(ahora, flota.intervalo(), flota.acelerar()).aMensaje(config.nodoId());
            generados.incrementAndGet();
            agregar(mensaje, sinCobertura);
            if (azar.nextDouble() < flota.duplicados()) {
                // el mismo mensaje, con el mismo idEvento: el central lo tiene que contar como DUPLICADO
                agregar(mensaje, sinCobertura);
                duplicados.incrementAndGet();
            }
        }
    }

    // lo llama un unico hilo cada segundo: publica de a lotes hasta vaciar lo pendiente o hasta que falle
    public void publicarPendientes(Instant ahora) {
        if (ahora.isBefore(proximoIntento)) {
            return;
        }
        while (true) {
            List<String> lote = siguienteLote();
            if (lote.isEmpty()) {
                return;
            }
            List<String> aEnviar = lote;
            if (flota.desordenar()) {
                aEnviar = new ArrayList<>(lote);
                Collections.shuffle(aEnviar, azar);
            }
            try {
                publicador.publicar(aEnviar);
            } catch (PublicadorException e) {
                intentosFallidos++;
                reintentos.incrementAndGet();
                Duration espera = espera(intentosFallidos);
                proximoIntento = ahora.plus(espera);
                LOGGER.warning("nodo resultado=BROKER_NO_DISPONIBLE pendientes=" + cantidadPendiente()
                        + " reintento_en_ms=" + espera.toMillis() + " error=" + e.getCause());
                return;
            }
            // recien ahora se sacan: si fallo antes, siguen ahi para el proximo intento
            confirmar(lote.size());
            publicados.addAndGet(lote.size());
            intentosFallidos = 0;
        }
    }

    public JsonObject metricas() {
        return Json.createObjectBuilder()
                .add("vehiculos", vehiculos.size())
                .add("generados", generados.get())
                .add("duplicados", duplicados.get())
                .add("publicados", publicados.get())
                .add("pendientes", cantidadPendiente())
                .add("retenidosSinCobertura", cantidadRetenida())
                .add("reintentos", reintentos.get())
                .build();
    }

    // 1 s, 2 s, 4 s... hasta 30 s, mas hasta 1 s al azar para que varios nodos no reintenten todos juntos
    Duration espera(int intentos) {
        long ms = Math.min(1000L << Math.min(intentos - 1, 5), ESPERA_MAXIMA_MS);
        return Duration.ofMillis(ms + azar.nextInt(1000));
    }

    synchronized int cantidadPendiente() {
        return pendientes.size();
    }

    synchronized int cantidadRetenida() {
        return retenidos.size();
    }

    private void agregar(String mensaje, boolean sinCobertura) {
        if (sinCobertura) {
            retenidos.add(mensaje);
        } else {
            pendientes.addLast(mensaje);
        }
    }

    // los primeros del buffer, sin sacarlos
    private synchronized List<String> siguienteLote() {
        return pendientes.stream().limit(config.maxLote()).toList();
    }

    private synchronized void confirmar(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            pendientes.pollFirst();
        }
    }

    private boolean sinCobertura(Instant ahora) {
        if (flota.sinCoberturaDesde() == null) {
            return false;
        }
        Instant desde = inicio.plus(flota.sinCoberturaDesde());
        return !ahora.isBefore(desde) && ahora.isBefore(desde.plus(flota.sinCoberturaDurante()));
    }
}
