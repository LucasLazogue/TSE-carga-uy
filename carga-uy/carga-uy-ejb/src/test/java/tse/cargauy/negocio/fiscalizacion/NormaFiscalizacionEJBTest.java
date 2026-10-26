package tse.cargauy.negocio.fiscalizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import jakarta.ejb.SessionContext;
import tse.cargauy.data.fiscalizacion.CasoFiscalizacionDAO;
import tse.cargauy.dtos.NormaFiscalizacionDto;
import tse.cargauy.entities.NormaFiscalizacion;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class NormaFiscalizacionEJBTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    private final List<NormaFiscalizacionDto> normas = new ArrayList<>();
    private final NormaFiscalizacionEJB ejb = new NormaFiscalizacionEJB() {
        @Override
        LocalDate hoy() {
            return HOY;
        }
    };

    @BeforeEach
    void iniciar() {
        ejb.contexto = (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { SessionContext.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "getCallerPrincipal" -> (Principal) () -> "1";
                    default -> null;
                });
        ejb.casoDAO = new CasoFiscalizacionDAO() {
            @Override
            public void addNorma(String clave, int valor, LocalDate vigenteDesde) {
                NormaFiscalizacionDto norma = new NormaFiscalizacionDto();
                norma.setId((long) normas.size() + 1);
                norma.setClave(clave);
                norma.setValor(valor);
                norma.setVigenteDesde(vigenteDesde);
                normas.add(norma);
            }

            @Override
            public List<NormaFiscalizacionDto> getNormas(String clave) {
                return normas.stream()
                        .filter(n -> n.getClave().equals(clave))
                        .sorted(Comparator.comparing(NormaFiscalizacionDto::getVigenteDesde)
                                .thenComparing(NormaFiscalizacionDto::getId).reversed())
                        .toList();
            }
        };
        ejb.casoDAO.addNorma(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, 5, LocalDate.of(2026, 1, 1));
        ejb.casoDAO.addNorma(NormaFiscalizacion.DESVIO_RUTA_DISTANCIA, 2000, LocalDate.of(2026, 1, 1));
    }

    @Test
    void vigentesDejaAfueraLasQueRigenMasAdelante() {
        ejb.cambiarValor(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, 8, HOY.plusDays(1));

        List<NormaFiscalizacionDto> vigentes = ejb.getVigentes();

        assertEquals(List.of(5, 2000), vigentes.stream().map(NormaFiscalizacionDto::getValor).toList());
    }

    @Test
    void cambiarDesdeHoyReemplazaLaVigente() {
        ejb.cambiarValor(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, 3, HOY);

        assertEquals(3, ejb.getVigentes().get(0).getValor());
        assertEquals(List.of(3, 5), ejb.getHistorial(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA).stream()
                .map(NormaFiscalizacionDto::getValor).toList());
    }

    @Test
    void cambiarDosVecesElMismoDiaQuedaLaUltima() {
        ejb.cambiarValor(NormaFiscalizacion.DESVIO_RUTA_DISTANCIA, 1500, HOY);
        ejb.cambiarValor(NormaFiscalizacion.DESVIO_RUTA_DISTANCIA, 1000, HOY);

        assertEquals(1000, ejb.getVigentes().get(1).getValor());
    }

    @Test
    void rechazaUnaClaveQueNoEsNorma() {
        assertError(CodigoError.NORMA_NO_ENCONTRADA, () -> ejb.cambiarValor("OTRA", 1, HOY));
        assertError(CodigoError.NORMA_NO_ENCONTRADA, () -> ejb.getHistorial(null));
    }

    @Test
    void rechazaUnValorNegativo() {
        assertError(CodigoError.NORMA_VALOR_INVALIDO,
                () -> ejb.cambiarValor(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, -1, HOY));
    }

    @Test
    void rechazaUnaVigenciaPasadaOVacia() {
        assertError(CodigoError.NORMA_VIGENCIA_INVALIDA,
                () -> ejb.cambiarValor(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, 5, HOY.minusDays(1)));
        assertError(CodigoError.NORMA_VIGENCIA_INVALIDA,
                () -> ejb.cambiarValor(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, 5, null));
        assertEquals(1, ejb.getHistorial(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA).size());
    }

    private static void assertError(CodigoError codigo, Executable accion) {
        assertEquals(codigo, assertThrows(CargaUYException.class, accion).getCodigo());
    }
}
