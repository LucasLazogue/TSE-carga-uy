package tse.cargauy.negocio.historico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.historico.HistoricoDAO;

class ReconstructorHistoricoTest {

    private final ReconstructorHistorico reconstructor = new ReconstructorHistorico();
    private final List<String> llamadas = new ArrayList<>();
    private RuntimeException alAgregarViajes;

    @BeforeEach
    void iniciar() {
        reconstructor.historicoDAO = new HistoricoDAO() {
            @Override
            public int agregarViajesFaltantes() {
                if (alAgregarViajes != null) {
                    throw alAgregarViajes;
                }
                llamadas.add("viajes");
                return 2;
            }

            @Override
            public int actualizarCasos() {
                llamadas.add("casos");
                return 1;
            }
        };
    }

    @Test
    void completaViajesYCasos() {
        reconstructor.reconstruir();

        assertEquals(List.of("viajes", "casos"), llamadas);
    }

    @Test
    void correCadaVezPorqueNoPisaNada() {
        reconstructor.reconstruir();
        reconstructor.reconstruir();

        assertEquals(List.of("viajes", "casos", "viajes", "casos"), llamadas);
    }

    @Test
    void siFallaNoTiraAbajoElArranque() {
        alAgregarViajes = new IllegalStateException("sin conexion");

        reconstructor.reconstruir();

        assertTrue(llamadas.isEmpty());
    }
}
