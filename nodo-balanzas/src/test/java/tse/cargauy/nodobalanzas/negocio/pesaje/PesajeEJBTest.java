package tse.cargauy.nodobalanzas.negocio.pesaje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.nodobalanzas.data.pesaje.PesajeDAO;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;
import tse.cargauy.nodobalanzas.exceptions.CodigoError;
import tse.cargauy.nodobalanzas.exceptions.NodoBalanzasException;

class PesajeEJBTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 20, 9, 11);

    private final List<String> guardados = new ArrayList<>();
    private String consulta;
    private final PesajeEJB ejb = new PesajeEJB() {
        @Override
        String codigoBalanza() {
            return "balanza-r5-km30";
        }

        @Override
        LocalDateTime ahora() {
            return AHORA;
        }
    };

    @BeforeEach
    void iniciar() {
        ejb.pesajeDAO = new PesajeDAO() {
            @Override
            public List<PesajeDto> getPesajesByMatricula(String matricula, LocalDateTime desde, LocalDateTime hasta) {
                consulta = matricula + " " + desde + " " + hasta;
                return List.of();
            }

            @Override
            public boolean existePesaje(String codigoBalanza, String idRegistro) {
                return guardados.contains(codigoBalanza + "/" + idRegistro);
            }

            @Override
            public void addPesaje(String codigoBalanza, PesajeDto pesajeDto, LocalDateTime recibidoEn) {
                guardados.add(codigoBalanza + "/" + pesajeDto.getIdRegistro());
            }
        };
    }

    @Test
    void consultaSinMatriculaFalla() {
        assertError(CodigoError.PESAJE_MATRICULA_REQUERIDA, () -> ejb.getPesajes(" ", FECHA));
    }

    @Test
    void consultaSinFechaFalla() {
        assertError(CodigoError.PESAJE_FECHA_REQUERIDA, () -> ejb.getPesajes("SCC9012", null));
    }

    @Test
    void consultaBuscaEseDiaCompleto() {
        ejb.getPesajes("SCC9012", FECHA);

        assertEquals("SCC9012 2026-10-20T00:00 2026-10-21T00:00", consulta);
    }

    @Test
    void sinLecturasFalla() {
        assertError(CodigoError.PESAJE_LECTURAS_REQUERIDAS, () -> ejb.addPesajes(List.of()));
    }

    @Test
    void lecturaSinIdRegistroFalla() {
        assertError(CodigoError.PESAJE_REGISTRO_REQUERIDO, () -> ejb.addPesajes(List.of(lectura(null, 36500))));
    }

    @Test
    void lecturaConPesoCeroFalla() {
        assertError(CodigoError.PESAJE_PESO_INVALIDO, () -> ejb.addPesajes(List.of(lectura("1001", 0))));
    }

    @Test
    void unaLecturaInvalidaNoGuardaNingunaDelLote() {
        assertThrows(NodoBalanzasException.class, () -> ejb.addPesajes(List.of(lectura("1001", 36500), lectura("1002", 0))));

        assertTrue(guardados.isEmpty());
    }

    @Test
    void guardaLasLecturasConLaBalanzaQueLasMando() {
        ejb.addPesajes(List.of(lectura("1001", 36500), lectura("1002", 20000)));

        assertEquals(List.of("balanza-r5-km30/1001", "balanza-r5-km30/1002"), guardados);
    }

    @Test
    void unReenvioNoSeGuardaDosVeces() {
        ejb.addPesajes(List.of(lectura("1001", 36500)));
        ejb.addPesajes(List.of(lectura("1001", 36500), lectura("1002", 20000)));

        assertEquals(List.of("balanza-r5-km30/1001", "balanza-r5-km30/1002"), guardados);
    }

    private static PesajeDto lectura(String idRegistro, int peso) {
        return new PesajeDto(idRegistro, "SCC9012", LocalDateTime.of(2026, 10, 20, 9, 10), peso);
    }

    private static void assertError(CodigoError codigo, Executable accion) {
        assertEquals(codigo, assertThrows(NodoBalanzasException.class, accion).getCodigo());
    }
}
