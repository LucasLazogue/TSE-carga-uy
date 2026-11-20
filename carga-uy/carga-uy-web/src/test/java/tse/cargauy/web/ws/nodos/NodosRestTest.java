package tse.cargauy.web.ws.nodos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.nodo.NodoPerifericoEJBLocal;
import tse.cargauy.web.ws.ApiEnMemoria;
import tse.cargauy.web.ws.EjbFalso;

class NodosRestTest {

    private final EjbFalso<NodoPerifericoEJBLocal> ejb = EjbFalso.de(NodoPerifericoEJBLocal.class);
    private ApiEnMemoria api;

    @BeforeEach
    void preparar() {
        NodosRest rest = new NodosRest();
        rest.nodoEJB = ejb.ejb();
        api = new ApiEnMemoria(rest);
    }

    @Test
    void listaYConsulta() throws Exception {
        ejb.responde("getVigentes", List.of(new NodoPerifericoDto("nodo-1", TipoNodo.values()[0], "https://nodo-1")))
                .responde("getById", new NodoPerifericoDto());

        MockHttpResponse lista = api.get("/nodos");

        assertEquals(200, lista.getStatus());
        assertTrue(lista.getContentAsString().contains("nodo-1"));
        assertEquals(200, api.get("/nodos/3").getStatus());
        assertEquals(3L, ejb.argumento("getById", 0));
    }

    @Test
    void alta() throws Exception {
        assertEquals(204, api.post("/nodos", "{\"identificador\":\"nodo-2\",\"puntoAcceso\":\"https://nodo-2\"}").getStatus());

        assertEquals("nodo-2", ((NodoPerifericoDto) ejb.argumento("addNodo", 0)).getIdentificador());
    }

    // el PUT del nodo solo cambia el punto de acceso, y el de la clave solo la clave
    @Test
    void modificarSoloTomaElCampoDeCadaOperacion() throws Exception {
        assertEquals(204, api.put("/nodos/3", "{\"puntoAcceso\":\"https://nuevo\",\"clave\":\"no-se-usa\"}").getStatus());
        assertEquals(204, api.put("/nodos/3/clave", "{\"clave\":\"secreta\",\"puntoAcceso\":\"no-se-usa\"}").getStatus());

        assertEquals(List.of(3L, "https://nuevo"), List.of(ejb.argumentos("cambiarPuntoAcceso")));
        assertEquals(List.of(3L, "secreta"), List.of(ejb.argumentos("cambiarClave")));
    }

    @Test
    void habilitarDeshabilitarYBaja() throws Exception {
        assertEquals(204, api.post("/nodos/3/habilitar", "").getStatus());
        assertEquals(204, api.post("/nodos/3/deshabilitar", "").getStatus());
        assertEquals(204, api.delete("/nodos/3").getStatus());

        assertEquals(List.of("habilitar", "deshabilitar", "darDeBaja"), ejb.llamadas());
    }

    @Test
    void unNodoInexistente() throws Exception {
        ejb.falla("habilitar", new CargaUYException(CodigoError.NODO_NO_ENCONTRADO, "9"));

        assertEquals(404, api.post("/nodos/9/habilitar", "").getStatus());
    }
}
