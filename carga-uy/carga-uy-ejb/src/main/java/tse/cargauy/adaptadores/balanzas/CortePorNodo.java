package tse.cargauy.adaptadores.balanzas;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

// corte ante fallas sostenidas, uno por nodo de balanzas (AC13). El @CircuitBreaker de MicroProfile guarda un estado
// por metodo, y como un mismo metodo consulta a todos los nodos, con uno caido se dejaba de preguntar tambien a los
// que funcionan. Mismo criterio que la anotacion: se abre si fallan 5 de las ultimas 10 llamadas, queda abierto 60 s
// y despues deja pasar una sola llamada de prueba: si anda se cierra, si falla vuelve a abrirse otros 60 s.
// Lo comparten los hilos del ejecutor: cada nodo tiene su estado y se sincroniza sobre el
final class CortePorNodo {

    static final int VENTANA = 10;
    static final int FALLAS_PARA_ABRIR = 5;
    static final long ABIERTO_MS = 60_000;

    enum Estado {
        CERRADO(0), MEDIO_ABIERTO(0.5), ABIERTO(1);

        // para la metrica del tablero, igual que la del corte de MicroProfile: 1 abierto, 0,5 probando
        final double valor;

        Estado(double valor) {
            this.valor = valor;
        }
    }

    private final Map<String, Nodo> nodos = new ConcurrentHashMap<>();
    private final LongSupplier relojMs;

    CortePorNodo() {
        this(() -> System.nanoTime() / 1_000_000);
    }

    CortePorNodo(LongSupplier relojMs) {
        this.relojMs = relojMs;
    }

    // true si se puede llamar al nodo. Pasados los 60 s, la primera que pregunta hace la llamada de prueba y las
    // demas siguen cortadas hasta que esa termine
    boolean permite(String nodo) {
        Nodo n = nodo(nodo);
        long ahora = relojMs.getAsLong();
        synchronized (n) {
            switch (n.estado) {
                case CERRADO:
                    return true;
                case ABIERTO:
                    if (ahora < n.hasta) {
                        return false;
                    }
                    n.estado = Estado.MEDIO_ABIERTO;
                    n.hasta = ahora + ABIERTO_MS;
                    return true;
                default:
                    // una prueba que nunca informo su resultado (hilo cancelado) no deja el nodo cortado para siempre
                    if (ahora < n.hasta) {
                        return false;
                    }
                    n.hasta = ahora + ABIERTO_MS;
                    return true;
            }
        }
    }

    // el nodo respondio, aunque sea con un rechazo (4xx): no es una falla del nodo
    void exito(String nodo) {
        Nodo n = nodo(nodo);
        synchronized (n) {
            if (n.estado == Estado.MEDIO_ABIERTO) {
                n.cerrar();
            } else if (n.estado == Estado.CERRADO) {
                n.registrar(false);
            }
        }
    }

    void falla(String nodo) {
        Nodo n = nodo(nodo);
        long ahora = relojMs.getAsLong();
        synchronized (n) {
            if (n.estado == Estado.MEDIO_ABIERTO) {
                n.abrir(ahora);
            } else if (n.estado == Estado.CERRADO) {
                n.registrar(true);
                if (n.cantidad == VENTANA && n.fallas >= FALLAS_PARA_ABRIR) {
                    n.abrir(ahora);
                }
            }
        }
    }

    Estado estado(String nodo) {
        Nodo n = nodo(nodo);
        synchronized (n) {
            return n.estado;
        }
    }

    // los nodos que ya se consultaron alguna vez, para publicar su estado
    boolean conoce(String nodo) {
        return nodos.containsKey(nodo);
    }

    private Nodo nodo(String nodo) {
        return nodos.computeIfAbsent(nodo, id -> new Nodo());
    }

    // las ultimas VENTANA llamadas, en un arreglo circular: true es una falla
    private static final class Nodo {
        private final boolean[] ultimas = new boolean[VENTANA];
        private int cantidad;
        private int siguiente;
        private int fallas;
        private Estado estado = Estado.CERRADO;
        private long hasta;

        void registrar(boolean falla) {
            if (cantidad == VENTANA && ultimas[siguiente]) {
                fallas--;
            }
            ultimas[siguiente] = falla;
            if (falla) {
                fallas++;
            }
            siguiente = (siguiente + 1) % VENTANA;
            cantidad = Math.min(cantidad + 1, VENTANA);
        }

        void abrir(long ahora) {
            estado = Estado.ABIERTO;
            hasta = ahora + ABIERTO_MS;
            vaciar();
        }

        void cerrar() {
            estado = Estado.CERRADO;
            vaciar();
        }

        private void vaciar() {
            Arrays.fill(ultimas, false);
            cantidad = 0;
            siguiente = 0;
            fallas = 0;
        }
    }
}
