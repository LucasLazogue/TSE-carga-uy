package tse.cargauy.negocio.publico;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import tse.cargauy.dtos.IndicadoresPublicosDto;

// Los indicadores publicos son agregados de meses, iguales para cualquiera que los pida con el mismo filtro, y
// calcularlos recorre todos los viajes del periodo. En la prueba de performance era la consulta que mas CPU le sacaba
// a PostgreSQL, por encima de la ingesta de posiciones (docs/performance.md). Cada instancia del central guarda el
// resultado unos minutos: un agregado de meses con minutos de atraso no le cambia nada a quien lo consulta.
// Si llegan varios pedidos iguales mientras se calcula, esperan ese mismo calculo en lugar de repetirlo.
public class CacheIndicadoresPublicos {

    static final Duration VIGENCIA = Duration.ofMinutes(5);
    // cualquiera puede pedir cualquier rango de fechas: tope para que el mapa no crezca sin limite
    static final int MAXIMO = 1000;

    // "hoy" entra en la clave porque los habilitados se cuentan a la fecha del dia
    public record Clave(LocalDate hoy, LocalDate desde, LocalDate hasta, Long idTipoCarga, Long idRubro) {
    }

    private record Entrada(CompletableFuture<IndicadoresPublicosDto> valor, long venceNanos) {
        boolean vencida(long ahora) {
            return ahora - venceNanos >= 0;
        }
    }

    private final ConcurrentHashMap<Clave, Entrada> entradas = new ConcurrentHashMap<>();
    private final LongSupplier reloj;

    public CacheIndicadoresPublicos() {
        this(System::nanoTime);
    }

    CacheIndicadoresPublicos(LongSupplier reloj) {
        this.reloj = reloj;
    }

    public IndicadoresPublicosDto obtener(Clave clave, Supplier<IndicadoresPublicosDto> calcular) {
        long ahora = reloj.getAsLong();
        Entrada nueva = new Entrada(new CompletableFuture<>(), ahora + VIGENCIA.toNanos());
        Entrada actual = entradas.compute(clave, (k, e) -> e == null || e.vencida(ahora) ? nueva : e);
        if (actual != nueva) {
            return esperar(actual.valor());
        }
        if (entradas.size() > MAXIMO) {
            purgar(ahora);
        }
        try {
            IndicadoresPublicosDto valor = calcular.get();
            nueva.valor().complete(valor);
            return valor;
        } catch (RuntimeException e) {
            // un error no se guarda: el proximo pedido vuelve a intentar
            entradas.remove(clave, nueva);
            nueva.valor().completeExceptionally(e);
            throw e;
        }
    }

    int tamanio() {
        return entradas.size();
    }

    private void purgar(long ahora) {
        entradas.values().removeIf(e -> e.vencida(ahora));
        if (entradas.size() > MAXIMO) {
            entradas.clear();
        }
    }

    private static IndicadoresPublicosDto esperar(CompletableFuture<IndicadoresPublicosDto> valor) {
        try {
            return valor.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw e;
        }
    }
}
