package tse.cargauy.negocio.publico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.IndicadoresPublicosDto;

class CacheIndicadoresPublicosTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 19);

    private final AtomicLong ahora = new AtomicLong();
    private final AtomicInteger calculos = new AtomicInteger();
    private final CacheIndicadoresPublicos cache = new CacheIndicadoresPublicos(ahora::get);

    private static CacheIndicadoresPublicos.Clave clave(int mes) {
        return new CacheIndicadoresPublicos.Clave(HOY, LocalDate.of(2026, mes, 1), LocalDate.of(2026, 12, 31), null, null);
    }

    private IndicadoresPublicosDto calcular() {
        calculos.incrementAndGet();
        return new IndicadoresPublicosDto();
    }

    @Test
    void dentroDeLaVigenciaDevuelveLoGuardado() {
        IndicadoresPublicosDto primero = cache.obtener(clave(1), this::calcular);
        ahora.addAndGet(CacheIndicadoresPublicos.VIGENCIA.toNanos() - 1);

        assertSame(primero, cache.obtener(clave(1), this::calcular));
        assertEquals(1, calculos.get());
    }

    @Test
    void vencidaVuelveACalcular() {
        IndicadoresPublicosDto primero = cache.obtener(clave(1), this::calcular);
        ahora.addAndGet(CacheIndicadoresPublicos.VIGENCIA.toNanos());

        IndicadoresPublicosDto segundo = cache.obtener(clave(1), this::calcular);

        assertEquals(2, calculos.get());
        assertTrue(primero != segundo);
    }

    @Test
    void otroDiaEsOtraClave() {
        cache.obtener(clave(1), this::calcular);
        cache.obtener(new CacheIndicadoresPublicos.Clave(HOY.plusDays(1), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31), null, null), this::calcular);

        assertEquals(2, calculos.get());
    }

    @Test
    void unErrorNoQuedaGuardado() {
        assertThrows(IllegalStateException.class, () -> cache.obtener(clave(1), () -> {
            throw new IllegalStateException("base caida");
        }));

        cache.obtener(clave(1), this::calcular);

        assertEquals(1, calculos.get());
    }

    @Test
    void pedidosSimultaneosCalculanUnaSolaVez() throws Exception {
        CountDownLatch calculando = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(4);
        try {
            Future<IndicadoresPublicosDto> primero = hilos.submit(() -> cache.obtener(clave(1), () -> {
                calculando.countDown();
                esperar(soltar);
                return calcular();
            }));
            assertTrue(calculando.await(5, TimeUnit.SECONDS));
            Future<IndicadoresPublicosDto> segundo = hilos.submit(() -> cache.obtener(clave(1), this::calcular));
            Future<IndicadoresPublicosDto> tercero = hilos.submit(() -> cache.obtener(clave(1), this::calcular));
            soltar.countDown();

            assertSame(primero.get(5, TimeUnit.SECONDS), segundo.get(5, TimeUnit.SECONDS));
            assertSame(primero.get(), tercero.get(5, TimeUnit.SECONDS));
            assertEquals(1, calculos.get());
        } finally {
            hilos.shutdownNow();
        }
    }

    @Test
    void noCreceSinLimite() {
        for (int i = 0; i <= CacheIndicadoresPublicos.MAXIMO + 10; i++) {
            LocalDate desde = LocalDate.of(2020, 1, 1).plusDays(i);
            cache.obtener(new CacheIndicadoresPublicos.Clave(HOY, desde, HOY, null, null), this::calcular);
        }

        assertTrue(cache.tamanio() <= CacheIndicadoresPublicos.MAXIMO + 1);
    }

    private static void esperar(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
