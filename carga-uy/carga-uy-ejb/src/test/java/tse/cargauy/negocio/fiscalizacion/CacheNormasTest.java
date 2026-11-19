package tse.cargauy.negocio.fiscalizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class CacheNormasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 19);

    private final AtomicLong ahora = new AtomicLong();
    private final AtomicInteger lecturas = new AtomicInteger();
    private final CacheNormas cache = new CacheNormas(ahora::get);
    private Integer enLaBase = 5000;

    private Integer leer() {
        lecturas.incrementAndGet();
        return enLaBase;
    }

    @Test
    void dentroDelMinutoNoVuelveALeer() {
        cache.vigente("DESVIO", HOY, this::leer);
        ahora.addAndGet(CacheNormas.VIGENCIA.toNanos() - 1);

        assertEquals(5000, cache.vigente("DESVIO", HOY, this::leer));
        assertEquals(1, lecturas.get());
    }

    @Test
    void pasadoElMinutoTomaLaVersionNueva() {
        cache.vigente("DESVIO", HOY, this::leer);
        enLaBase = 3000;
        ahora.addAndGet(CacheNormas.VIGENCIA.toNanos());

        assertEquals(3000, cache.vigente("DESVIO", HOY, this::leer));
        assertEquals(2, lecturas.get());
    }

    @Test
    void otraNormaUOtraFechaSeLeenAparte() {
        cache.vigente("DESVIO", HOY, this::leer);
        cache.vigente("TOLERANCIA", HOY, this::leer);
        cache.vigente("DESVIO", HOY.minusDays(1), this::leer);

        assertEquals(3, lecturas.get());
    }

    @Test
    void sinNormaVigenteNoSeGuarda() {
        enLaBase = null;

        assertNull(cache.vigente("DESVIO", HOY, this::leer));
        enLaBase = 5000;
        assertEquals(5000, cache.vigente("DESVIO", HOY, this::leer));
        assertEquals(2, lecturas.get());
    }
}
