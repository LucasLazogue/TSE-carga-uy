package tse.cargauy.negocio.fiscalizacion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import tse.cargauy.negocio.tracking.PosicionProcesada;
import tse.cargauy.negocio.viaje.ViajeIniciadoOFinalizado;

class DisparadorFiscalizacionTest {

    private final DisparadorFiscalizacion disparador = new DisparadorFiscalizacion();
    private final List<String> evaluaciones = new ArrayList<>();
    private final List<Context> trazas = new ArrayList<>();

    private void motor(RuntimeException falla) {
        disparador.motor = new MotorFiscalizacionEJBLocal() {
            @Override
            public void evaluarPosicion(Long idNodo, String idEvento, Context traza) {
                evaluaciones.add("posicion " + idNodo + " " + idEvento);
                trazas.add(traza);
                if (falla != null) {
                    throw falla;
                }
            }

            @Override
            public void evaluarViaje(Long idViaje, Context traza) {
                evaluaciones.add("viaje " + idViaje);
                trazas.add(traza);
                if (falla != null) {
                    throw falla;
                }
            }
        };
    }

    // la evaluacion cuelga de la traza de la ingesta, aunque corra despues de que el MDB cerro su span
    @Test
    void evaluaLaPosicionEnLaTrazaDeLaIngesta() {
        motor(null);
        Context traza = Context.root().with(ContextKey.named("ingesta"), "e-1");

        disparador.alProcesarPosicion(new PosicionProcesada(7L, "e-1", traza));

        assertEquals(List.of("posicion 7 e-1"), evaluaciones);
        assertSame(traza, trazas.get(0));
    }

    @Test
    void evaluaElViajeEnLaTrazaEnCurso() {
        motor(null);
        Context enCurso = Context.root().with(ContextKey.named("pedido"), "fin");

        try (var alcance = enCurso.makeCurrent()) {
            disparador.alConfirmarViaje(new ViajeIniciadoOFinalizado(3L));
        }

        assertEquals(List.of("viaje 3"), evaluaciones);
        assertSame(enCurso, trazas.get(0));
    }

    // la posicion o el fin del viaje ya se guardaron: una falla de la fiscalizacion no los afecta
    @Test
    void unaFallaDelMotorNoSePropaga() {
        motor(new IllegalStateException("regla rota"));

        assertDoesNotThrow(() -> disparador.alProcesarPosicion(new PosicionProcesada(7L, "e-1", Context.root())));
        assertDoesNotThrow(() -> disparador.alConfirmarViaje(new ViajeIniciadoOFinalizado(3L)));
        assertEquals(List.of("posicion 7 e-1", "viaje 3"), evaluaciones);
    }
}
