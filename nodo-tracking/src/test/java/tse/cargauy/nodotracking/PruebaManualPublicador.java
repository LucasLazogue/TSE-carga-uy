package tse.cargauy.nodotracking;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

// prueba contra el central levantado (docker compose up) y con el nodo tracking-1 registrado y habilitado.
// el nombre no termina en Test a proposito: mvn verify y la CI no la corren; se corre a mano con
// mvn test -Dtest=PruebaManualPublicador
class PruebaManualPublicador {

    @Test
    void publicaDosEventosEnElCentral() throws Exception {
        Config config = new Config("tracking-1", 8081, "remote+http://localhost:8080", "admin", "admin123", 500);
        // id distinto en cada corrida: si no, la segunda vez el central los cuenta como DUPLICADO
        String sufijo = String.valueOf(System.currentTimeMillis());
        try (PublicadorJms publicador = new PublicadorJms(config)) {
            publicador.publicar(List.of(
                    new EventoGps("manual-" + sufijo + "-1", "STA1234", -34.9011, -56.1645, Instant.now().minusSeconds(30))
                            .aMensaje("tracking-1"),
                    new EventoGps("manual-" + sufijo + "-2", "STA1234", -34.8500, -56.1000, Instant.now())
                            .aMensaje("tracking-1")));
        }
    }
}
