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

class PeajeEJBTest {

    private static final String CLAVE = "clave-de-la-balanza-1";

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
            public String getCodigoBalanzaByClave(String claveHash) {
                return clavesDeBalanzas.get(claveHash);
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
        assertError(CodigoError.PEAJE_NO_ENCONTRADO,
                () -> ejb.addBalanza("R9-99", new BalanzaDto("balanza-r9-km99", Sentido.HACIA_MONTEVIDEO, CLAVE)));
    }

    @Test
    void balanzaSinSentidoFalla() {
        assertError(CodigoError.BALANZA_SENTIDO_REQUERIDO,
                () -> ejb.addBalanza("R5-30", new BalanzaDto("balanza-r5-km30", null, CLAVE)));
    }

    @Test
    void balanzaConClaveCortaFalla() {
        assertError(CodigoError.BALANZA_CLAVE_CORTA,
                () -> ejb.addBalanza("R5-30", new BalanzaDto("balanza-r5-km30", Sentido.HACIA_MONTEVIDEO, "corta")));
    }

    @Test
    void balanzaRepetidaFalla() {
        ejb.addBalanza("R5-30", new BalanzaDto("balanza-r5-km30", Sentido.HACIA_MONTEVIDEO, CLAVE));
        assertError(CodigoError.BALANZA_CODIGO_DUPLICADO,
                () -> ejb.addBalanza("R5-30", new BalanzaDto("balanza-r5-km30", Sentido.HACIA_EL_INTERIOR, "otra-clave-de-balanza")));
    }

    @Test
    void dosBalanzasConLaMismaClaveFalla() {
        ejb.addBalanza("R5-30", new BalanzaDto("balanza-r5-km30", Sentido.HACIA_MONTEVIDEO, CLAVE));
        assertError(CodigoError.BALANZA_CLAVE_EN_USO,
                () -> ejb.addBalanza("R5-30", new BalanzaDto("balanza-r5-km30-b", Sentido.HACIA_EL_INTERIOR, CLAVE)));
    }

    @Test
    void laClaveDeLaBalanzaSeGuardaHasheada() {
        ejb.addBalanza("R5-30", new BalanzaDto("balanza-r5-km30", Sentido.HACIA_MONTEVIDEO, CLAVE));

        String guardada = clavesDeBalanzas.keySet().iterator().next();
        assertNotEquals(CLAVE, guardada);
        assertEquals("balanza-r5-km30", clavesDeBalanzas.get(guardada));
    }

    private static PeajeDto peaje(String codigo) {
        return new PeajeDto(codigo, "Peaje Ruta 5 km 30", "5", 30, -34.62, -56.25);
    }

    private static void assertError(CodigoError codigo, Executable accion) {
        assertEquals(codigo, assertThrows(NodoBalanzasException.class, accion).getCodigo());
    }
}
