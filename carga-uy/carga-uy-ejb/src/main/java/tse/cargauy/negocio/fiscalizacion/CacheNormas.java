package tse.cargauy.negocio.fiscalizacion;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;

// La norma vigente (distancia de desvio, tolerancia de exceso) se lee con cada posicion de un viaje en curso: a 600
// posiciones por segundo son 600 consultas por segundo a PostgreSQL por un valor que cambia muy de vez en cuando
// (docs/performance.md). Cada instancia la guarda un minuto: una version nueva cargada desde el backoffice rige, como
// mucho, un minuto despues. Un singleton por instancia; la concurrencia la resuelve el mapa
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class CacheNormas {

    static final Duration VIGENCIA = Duration.ofMinutes(1);

    record Clave(String norma, LocalDate fecha) {
    }

    private record Entrada(Integer valor, long venceNanos) {
    }

    private final ConcurrentHashMap<Clave, Entrada> entradas = new ConcurrentHashMap<>();
    private final LongSupplier reloj;

    public CacheNormas() {
        this(System::nanoTime);
    }

    CacheNormas(LongSupplier reloj) {
        this.reloj = reloj;
    }

    // sin norma vigente (null) no se guarda: la proxima consulta vuelve a leer la base
    public Integer vigente(String norma, LocalDate fecha, Supplier<Integer> leer) {
        Clave clave = new Clave(norma, fecha);
        long ahora = reloj.getAsLong();
        Entrada entrada = entradas.get(clave);
        if (entrada != null && ahora - entrada.venceNanos() < 0) {
            return entrada.valor();
        }
        Integer valor = leer.get();
        if (valor == null) {
            entradas.remove(clave);
        } else {
            // las fechas viejas dejan de pedirse: se limpian al guardar una nueva
            entradas.values().removeIf(e -> ahora - e.venceNanos() >= 0);
            entradas.put(clave, new Entrada(valor, ahora + VIGENCIA.toNanos()));
        }
        return valor;
    }
}
