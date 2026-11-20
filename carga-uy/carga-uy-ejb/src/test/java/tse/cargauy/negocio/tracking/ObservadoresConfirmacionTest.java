package tse.cargauy.negocio.tracking;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.opentelemetry.context.Context;
import tse.cargauy.data.posicion.EventoPosicionDAOLocal;
import tse.cargauy.negocio.viaje.ViajeIniciadoOFinalizado;

// los observadores que corren cuando ya se confirmo la transaccion: no pueden deshacerla, asi que una falla se
// registra y no se propaga
class ObservadoresConfirmacionTest {

    private final List<String> llamadas = new ArrayList<>();

    private EventoPosicionDAOLocal eventoDAO(RuntimeException falla) {
        return (EventoPosicionDAOLocal) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { EventoPosicionDAOLocal.class }, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("marcarProcesada")) {
                        llamadas.add("marcar " + args[0] + " " + args[1]);
                        if (falla != null) {
                            throw falla;
                        }
                    }
                    return null;
                });
    }

    @Test
    void marcaLaPosicionProcesadaAlConfirmar() {
        MarcadorPosiciones marcador = new MarcadorPosiciones();
        marcador.eventoDAO = eventoDAO(null);

        marcador.alConfirmar(new PosicionProcesada(7L, "e-1", Context.root()));

        assertEquals(List.of("marcar 7 e-1"), llamadas);
    }

    // queda sin marcar: un reenvio repite los pasos siguientes, que son idempotentes
    @Test
    void siNoPuedeMarcarNoPropagaLaFalla() {
        MarcadorPosiciones marcador = new MarcadorPosiciones();
        marcador.eventoDAO = eventoDAO(new IllegalStateException("sin base"));

        assertDoesNotThrow(() -> marcador.alConfirmar(new PosicionProcesada(7L, "e-1", Context.root())));
        assertEquals(List.of("marcar 7 e-1"), llamadas);
    }

    @Test
    void reasociaLasPosicionesAlConfirmarElViaje() {
        ReasociacionPosiciones reasociacion = new ReasociacionPosiciones();
        reasociacion.reasociador = new ReasociadorPosiciones() {
            @Override
            public void reasociar(Long idViaje) {
                llamadas.add("reasociar " + idViaje);
            }
        };

        reasociacion.alConfirmar(new ViajeIniciadoOFinalizado(3L));

        assertEquals(List.of("reasociar 3"), llamadas);
    }

    @Test
    void siNoPuedeReasociarNoPropagaLaFalla() {
        ReasociacionPosiciones reasociacion = new ReasociacionPosiciones();
        reasociacion.reasociador = new ReasociadorPosiciones() {
            @Override
            public void reasociar(Long idViaje) {
                throw new IllegalStateException("sin base");
            }
        };

        assertDoesNotThrow(() -> reasociacion.alConfirmar(new ViajeIniciadoOFinalizado(3L)));
    }
}
