package tse.cargauy.nodobalanzas.negocio.peaje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.nodobalanzas.data.peaje.PeajeDAO;
import tse.cargauy.nodobalanzas.dtos.BalanzaDto;
import tse.cargauy.nodobalanzas.dtos.PeajeDto;
import tse.cargauy.nodobalanzas.entities.Sentido;
import tse.cargauy.nodobalanzas.exceptions.CodigoError;
import tse.cargauy.nodobalanzas.exceptions.NodoBalanzasException;
import tse.cargauy.nodobalanzas.seguridad.Sha256;

class PeajeEJBTest {

    private final PeajeEJB ejb = new PeajeEJB();
    private final Map<String, PeajeDto> peajes = new HashMap<>();
    private final Map<String, String> clavesDeBalanzas = new HashMap<>();

    @BeforeEach
    void iniciar() {
        ejb.peajeDAO = new PeajeDAO() {
            @Override
            public PeajeDto getPeajeByCodigo(String codigo) {
                return peajes.get(codigo);
            }

            @Override
            public void addPeaje(PeajeDto peajeDto) {
                peajes.put(peajeDto.getCodigo(), peajeDto);
            }

            @Override
            public BalanzaDto getBalanzaByCodigo(String codigo) {
                return clavesDeBalanzas.containsValue(codigo) ? new BalanzaDto(codigo, Sentido.HACIA_MONTEVIDEO, null) : null;
            }

            @Override
            public void addBalanza(String codigoPeaje, BalanzaDto balanzaDto, String claveHash) {
                clavesDeBalanzas.put(claveHash, balanzaDto.getCodigo());
            }
        };
        ejb.addPeaje(peaje("R5-30"));
    }

    @Test
    void peajeSinCodigoFalla() {
        assertError(CodigoError.PEAJE_CODIGO_REQUERIDO, () -> ejb.addPeaje(peaje(" ")));
    }

    @Test
    void peajeConKmNegativoFalla() {
        PeajeDto peaje = peaje("R1-45");
        peaje.setKm(-1);
        assertError(CodigoError.PEAJE_KM_INVALIDO, () -> ejb.addPeaje(peaje));
    }

    @Test
    void peajeRepetidoFalla() {
        assertError(CodigoError.PEAJE_CODIGO_DUPLICADO, () -> ejb.addPeaje(peaje("R5-30")));
    }

    @Test
    void balanzaEnPeajeInexistenteFalla() {
        assertError(CodigoError.PEAJE_NO_ENCONTRADO, () -> ejb.addBalanza("R9-99", balanza("balanza-r9-km99")));
    }

    @Test
    void balanzaSinSentidoFalla() {
        assertError(CodigoError.BALANZA_SENTIDO_REQUERIDO,
                () -> ejb.addBalanza("R5-30", new BalanzaDto("balanza-r5-km30", null, null)));
    }

    @Test
    void balanzaRepetidaFalla() {
        ejb.addBalanza("R5-30", balanza("balanza-r5-km30"));
        assertError(CodigoError.BALANZA_CODIGO_DUPLICADO, () -> ejb.addBalanza("R5-30", balanza("balanza-r5-km30")));
    }

    @Test
    void elNodoGeneraLaClaveYGuardaSoloSuHash() {
        String clave = ejb.addBalanza("R5-30", balanza("balanza-r5-km30")).getClave();

        assertEquals(43, clave.length());
        assertEquals("balanza-r5-km30", clavesDeBalanzas.get(Sha256.base64Url(clave)));
        assertNotEquals(clave, clavesDeBalanzas.keySet().iterator().next());
    }

    @Test
    void cadaBalanzaRecibeUnaClaveDistinta() {
        String primera = ejb.addBalanza("R5-30", balanza("balanza-r5-km30")).getClave();
        String segunda = ejb.addBalanza("R5-30", balanza("balanza-r5-km30-b")).getClave();

        assertNotEquals(primera, segunda);
    }

    private static PeajeDto peaje(String codigo) {
        return new PeajeDto(codigo, "Peaje Ruta 5 km 30", "5", 30, -34.62, -56.25);
    }

    private static BalanzaDto balanza(String codigo) {
        return new BalanzaDto(codigo, Sentido.HACIA_MONTEVIDEO, null);
    }

    private static void assertError(CodigoError codigo, Executable accion) {
        assertEquals(codigo, assertThrows(NodoBalanzasException.class, accion).getCodigo());
    }
}
