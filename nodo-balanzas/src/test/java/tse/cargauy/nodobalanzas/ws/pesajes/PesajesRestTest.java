package tse.cargauy.nodobalanzas.ws.pesajes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tse.cargauy.nodobalanzas.dtos.PesajeDto;
import tse.cargauy.nodobalanzas.negocio.pesaje.PesajeEJBLocal;

class PesajesRestTest {

    @Test
    void consultaYRecibeLecturas() {
        List<String> llamadas = new ArrayList<>();
        List<PesajeDto> lista = List.of(new PesajeDto());
        PesajesRest rest = new PesajesRest();
        rest.pesajeEJB = (PesajeEJBLocal) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { PesajeEJBLocal.class },
                (proxy, metodo, args) -> {
                    llamadas.add(metodo.getName() + " " + args[0] + (args.length > 1 ? " " + args[1] : ""));
                    return lista;
                });

        assertSame(lista, rest.getPesajes("STA1234", LocalDate.of(2026, 10, 20)));
        rest.createPesajes(lista);

        assertEquals(List.of("getPesajes STA1234 2026-10-20", "addPesajes " + lista), llamadas);
    }
}
