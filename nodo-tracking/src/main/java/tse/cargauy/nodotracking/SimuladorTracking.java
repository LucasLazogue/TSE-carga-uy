package tse.cargauy.nodotracking;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;

// Simula el componente periferico del Sistema de Tracking: genera posiciones de vehiculos y las publica en la cola
// queue_tracking del componente central. Puede reenviar eventos y mandarlos desordenados para probar la ingesta.
public class SimuladorTracking {

    // Montevideo -> Colonia, en linea recta: alcanza para probar, no pretende ser una ruta real
    private static final double LAT_ORIGEN = -34.9011;
    private static final double LON_ORIGEN = -56.1645;
    private static final double LAT_DESTINO = -34.4626;
    private static final double LON_DESTINO = -57.8400;

    public static void main(String[] args) throws NamingException {
        Map<String, String> opciones = leerOpciones(args);
        String url = opciones.getOrDefault("url", "remote+http://localhost:8080");
        String nodo = opciones.getOrDefault("nodo", "tracking-sim");
        String[] vehiculos = opciones.getOrDefault("vehiculos", "STA1234,SBB5678").split(",");
        int eventos = Integer.parseInt(opciones.getOrDefault("eventos", "10"));
        double duplicados = Double.parseDouble(opciones.getOrDefault("duplicados", "0.3"));
        int intervalo = Integer.parseInt(opciones.getOrDefault("intervalo", "10"));
        boolean desordenar = opciones.containsKey("desordenar");

        // las credenciales no van en el codigo (R11): se leen del entorno
        String usuario = System.getenv("NODO_JMS_USUARIO");
        String clave = System.getenv("NODO_JMS_CLAVE");
        if (usuario == null || clave == null) {
            System.err.println("Faltan las variables de entorno NODO_JMS_USUARIO y NODO_JMS_CLAVE.");
            System.exit(1);
        }

        Random random = new Random();
        List<String> mensajes = new ArrayList<>();
        int unicos = 0;
        Instant inicio = Instant.now().minusSeconds((long) eventos * intervalo);
        for (String matricula : vehiculos) {
            for (int i = 0; i < eventos; i++) {
                double avance = (double) i / Math.max(1, eventos - 1);
                String json = evento(UUID.randomUUID().toString(), nodo, matricula.trim(),
                        LAT_ORIGEN + (LAT_DESTINO - LAT_ORIGEN) * avance,
                        LON_ORIGEN + (LON_DESTINO - LON_ORIGEN) * avance,
                        inicio.plusSeconds((long) i * intervalo));
                mensajes.add(json);
                unicos++;
                if (random.nextDouble() < duplicados) {
                    mensajes.add(json);
                }
            }
        }
        if (desordenar) {
            Collections.shuffle(mensajes, random);
        }

        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        props.setProperty(Context.PROVIDER_URL, url);
        props.setProperty(Context.SECURITY_PRINCIPAL, usuario);
        props.setProperty(Context.SECURITY_CREDENTIALS, clave);
        InitialContext ctx = new InitialContext(props);
        try {
            ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
            Queue cola = (Queue) ctx.lookup("jms/queue/queue_tracking");
            try (JMSContext jms = cf.createContext(usuario, clave)) {
                for (String mensaje : mensajes) {
                    jms.createProducer().send(cola, mensaje);
                }
            }
        } finally {
            ctx.close();
        }
        System.out.printf("Nodo %s: %d mensajes enviados (%d eventos distintos + %d reenvios)%s%n",
                nodo, mensajes.size(), unicos, mensajes.size() - unicos, desordenar ? ", en orden aleatorio" : "");
    }

    private static String evento(String idEvento, String nodo, String matricula, double lat, double lon, Instant generado) {
        return String.format(Locale.ROOT,
                "{\"idEvento\":\"%s\",\"nodo\":\"%s\",\"matricula\":\"%s\",\"latitud\":%.6f,\"longitud\":%.6f,\"timestampGeneracion\":\"%s\"}",
                texto(idEvento), texto(nodo), texto(matricula), lat, lon, generado);
    }

    private static String texto(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // --clave valor, o --clave sola para las opciones booleanas
    private static Map<String, String> leerOpciones(String[] args) {
        Map<String, String> opciones = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String clave = args[i].substring(2);
                boolean conValor = i + 1 < args.length && !args[i + 1].startsWith("--");
                opciones.put(clave, conValor ? args[++i] : "true");
            }
        }
        return opciones;
    }
}
