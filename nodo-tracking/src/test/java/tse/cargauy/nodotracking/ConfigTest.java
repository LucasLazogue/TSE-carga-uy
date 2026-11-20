package tse.cargauy.nodotracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

// la configuracion del nodo sale de variables de entorno; las de esta prueba las define surefire en el pom
class ConfigTest {

    @Test
    void leeElEntornoYCompletaConLosValoresPorDefecto() {
        Config config = Config.desdeEntorno();

        assertEquals("nodo-prueba", config.nodoId());
        assertEquals(9091, config.puerto());
        assertEquals("usuario-prueba", config.jmsUsuario());
        assertEquals("clave-prueba", config.jmsClave());
        assertTrue(config.jmsUrl().contains("sslEnabled=true"), "por defecto con TLS (R8)");
        assertEquals(500, config.maxLote());
        assertEquals(200_000, config.maxPendientes());
    }

    @Test
    void unaVariableObligatoriaQueFalta() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> Config.obligatoria("NODO_NO_DEFINIDA"));

        assertTrue(e.getMessage().contains("NODO_NO_DEFINIDA"));
    }

    @Test
    void unNumeroQueNoEsNumero() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> Config.numero("NODO_PRUEBA_NUMERO_INVALIDO", 1));

        assertTrue(e.getMessage().contains("entero"));
        assertThrows(IllegalStateException.class, () -> Config.decimal("NODO_PRUEBA_NUMERO_INVALIDO", 1));
    }

    @Test
    void valoresPorDefecto() {
        assertEquals("x", Config.texto("NODO_NO_DEFINIDA", "x"));
        assertEquals(7, Config.numero("NODO_NO_DEFINIDA", 7));
        assertEquals(0.5, Config.decimal("NODO_NO_DEFINIDA", 0.5));
        assertEquals(2.5, Config.decimal("NODO_ACELERAR", 1));
    }

    // el escenario de la prueba: vehiculos extra, aceleracion y un corte de cobertura "desde,durante" en minutos
    @Test
    void laFlotaDelEscenario() {
        ConfigFlota flota = ConfigFlota.desdeEntorno();

        assertEquals(3, flota.vehiculosExtra());
        assertEquals(2.5, flota.acelerar());
        assertEquals(Duration.ofMinutes(10), flota.sinCoberturaDesde());
        assertEquals(Duration.ofMinutes(5), flota.sinCoberturaDurante());
        assertEquals(Duration.ofSeconds(10), flota.intervalo());
        assertEquals(0, flota.duplicados());
        assertFalse(flota.desordenar());
        assertEquals(1, flota.primerVehiculo());
        assertEquals(null, flota.rutas(), "sin archivo usa las rutas incluidas");
    }
}
