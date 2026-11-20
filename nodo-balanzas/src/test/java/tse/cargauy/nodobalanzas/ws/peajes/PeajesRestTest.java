package tse.cargauy.nodobalanzas.ws.peajes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tse.cargauy.nodobalanzas.dtos.BalanzaDto;
import tse.cargauy.nodobalanzas.dtos.PeajeDto;
import tse.cargauy.nodobalanzas.entities.Sentido;
import tse.cargauy.nodobalanzas.negocio.peaje.PeajeEJBLocal;

class PeajesRestTest {

    @Test
    void peajesYBalanzas() {
        List<String> llamadas = new ArrayList<>();
        BalanzaDto balanza = new BalanzaDto("balanza-1", Sentido.HACIA_MONTEVIDEO, "clave");
        List<PeajeDto> peajes = List.of(new PeajeDto());
        PeajesRest rest = new PeajesRest();
        rest.peajeEJB = (PeajeEJBLocal) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { PeajeEJBLocal.class },
                (proxy, metodo, args) -> {
                    llamadas.add(metodo.getName());
                    return switch (metodo.getName()) {
                        case "getPeajes" -> peajes;
                        case "addBalanza" -> {
                            assertEquals("r5-km30", args[0]);
                            yield args[1];
                        }
                        default -> null;
                    };
                });

        assertSame(peajes, rest.getPeajes());
        rest.createPeaje(new PeajeDto());
        assertSame(balanza, rest.createBalanza("r5-km30", balanza));

        assertEquals(List.of("getPeajes", "addPeaje", "addBalanza"), llamadas);
    }
}
