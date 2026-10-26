package tse.cargauy.negocio.nodo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.data.nodo.NodoPerifericoDAO;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.seguridad.Aes;

class NodoPerifericoEJBTest {

    private static final String SECRETO = "secreto-de-prueba-de-al-menos-32-bytes";

    private final Map<String, NodoPerifericoDto> nodos = new HashMap<>();
    private final Map<Long, String> claves = new HashMap<>();
    private String secreto = SECRETO;

    private final NodoPerifericoEJB ejb = new NodoPerifericoEJB() {
        @Override
        String leerSecreto() {
            return secreto;
        }
    };

    @BeforeEach
    void iniciar() {
        ejb.nodoDAO = new NodoPerifericoDAO() {
            @Override
            public NodoPerifericoDto getById(Long id) {
                return nodos.values().stream().filter(n -> n.getId().equals(id)).findFirst().orElse(null);
            }

            @Override
            public NodoPerifericoDto getByIdentificador(String identificador) {
                return nodos.get(identificador);
            }

            @Override
            public void addNodo(NodoPerifericoDto nodoDto, String claveCifrada) {
                nodoDto.setId((long) nodos.size() + 1);
                nodos.put(nodoDto.getIdentificador(), nodoDto);
                claves.put(nodoDto.getId(), claveCifrada);
            }

            @Override
            public void cambiarClave(Long id, String claveCifrada) {
                claves.put(id, claveCifrada);
            }
        };
    }

    @Test
    void nodoDeBalanzasSinClaveFalla() {
        assertError(CodigoError.NODO_CLAVE_REQUERIDA, () -> ejb.addNodo(nodo("balanzas-sur", TipoNodo.BALANZAS, null)));
    }

    @Test
    void nodoDeTrackingNoNecesitaClave() {
        ejb.addNodo(nodo("tracking-1", TipoNodo.TRACKING, null));

        assertNull(claves.get(1L));
        assertEquals(EstadoNodo.DESHABILITADO, nodos.get("tracking-1").getEstado());
    }

    @Test
    void laClaveSeGuardaCifradaYSePuedeRecuperar() {
        ejb.addNodo(nodo("balanzas-sur", TipoNodo.BALANZAS, "clave-central-balanzas-sur"));

        String guardada = claves.get(1L);
        assertNotEquals("clave-central-balanzas-sur", guardada);
        assertEquals("clave-central-balanzas-sur", Aes.descifrar(guardada, SECRETO));
    }

    @Test
    void sinSecretoNoSeGuardaLaClave() {
        secreto = null;

        assertError(CodigoError.NODO_SECRETO_NO_CONFIGURADO,
                () -> ejb.addNodo(nodo("balanzas-sur", TipoNodo.BALANZAS, "clave-central-balanzas-sur")));
    }

    @Test
    void cambiarLaClaveLaVuelveACifrar() {
        ejb.addNodo(nodo("balanzas-sur", TipoNodo.BALANZAS, "clave-vieja"));

        ejb.cambiarClave(1L, "clave-nueva");

        assertEquals("clave-nueva", Aes.descifrar(claves.get(1L), SECRETO));
    }

    @Test
    void cambiarLaClaveVaciaFalla() {
        ejb.addNodo(nodo("balanzas-sur", TipoNodo.BALANZAS, "clave-vieja"));

        assertError(CodigoError.NODO_CLAVE_REQUERIDA, () -> ejb.cambiarClave(1L, " "));
    }

    private static NodoPerifericoDto nodo(String identificador, TipoNodo tipo, String clave) {
        NodoPerifericoDto nodo = new NodoPerifericoDto(identificador, tipo, "http://" + identificador + ":8080/nodo-balanzas");
        nodo.setClave(clave);
        return nodo;
    }

    private static void assertError(CodigoError codigo, Executable accion) {
        assertEquals(codigo, assertThrows(CargaUYException.class, accion).getCodigo());
    }
}
