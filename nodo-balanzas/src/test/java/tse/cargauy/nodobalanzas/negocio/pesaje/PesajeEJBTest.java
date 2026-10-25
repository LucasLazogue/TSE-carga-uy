package tse.cargauy.nodobalanzas.negocio.pesaje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.nodobalanzas.data.pesaje.PesajeDAO;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;
import tse.cargauy.nodobalanzas.exceptions.CodigoError;
import tse.cargauy.nodobalanzas.exceptions.NodoBalanzasException;

class PesajeEJBTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);

    private final PesajeEJB ejb = new PesajeEJB();
    private String consulta;

    @BeforeEach
    void iniciar() {
        ejb.pesajeDAO = new PesajeDAO() {
            @Override
            public List<PesajeDto> getPesajesByMatricula(String matricula, LocalDateTime desde, LocalDateTime hasta) {
                consulta = "matricula " + matricula + " " + desde + " " + hasta;
                return List.of();
            }

            @Override
            public List<PesajeDto> getPesajesByRut(String rut, LocalDateTime desde, LocalDateTime hasta) {
                consulta = "rut " + rut + " " + desde + " " + hasta;
                return List.of();
            }
        };
    }

    @Test
    void sinFechaFalla() {
        assertError(CodigoError.PESAJE_FECHA_REQUERIDA, () -> ejb.getPesajes("SCC9012", null, null));
    }

    @Test
    void sinMatriculaNiRutFalla() {
        assertError(CodigoError.PESAJE_MATRICULA_O_RUT, () -> ejb.getPesajes(null, " ", FECHA));
    }

    @Test
    void conMatriculaYRutFalla() {
        assertError(CodigoError.PESAJE_MATRICULA_O_RUT, () -> ejb.getPesajes("SCC9012", "211234560019", FECHA));
    }

    @Test
    void porMatriculaBuscaEseDiaCompleto() {
        ejb.getPesajes("SCC9012", null, FECHA);

        assertEquals("matricula SCC9012 2026-10-20T00:00 2026-10-21T00:00", consulta);
    }

    @Test
    void porRutBuscaEseDiaCompleto() {
        ejb.getPesajes(null, "211234560019", FECHA);

        assertEquals("rut 211234560019 2026-10-20T00:00 2026-10-21T00:00", consulta);
    }

    private static void assertError(CodigoError codigo, org.junit.jupiter.api.function.Executable accion) {
        assertEquals(codigo, assertThrows(NodoBalanzasException.class, accion).getCodigo());
    }
}
