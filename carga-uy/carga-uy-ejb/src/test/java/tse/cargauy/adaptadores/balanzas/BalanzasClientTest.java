package tse.cargauy.adaptadores.balanzas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.PesajeDto;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

// sin contenedor las anotaciones de tolerancia a fallas no hacen nada: aca se prueba la llamada al nodo y el
// pasaje de su respuesta al PesajeDto del central. la espera, los reintentos y el corte se prueban con el compose
class BalanzasClientTest {

    private static final String RESPUESTA = """
            [{"idRegistro":"1001","matricula":"SCC9012","fechaHora":"2026-10-22T09:10:00","pesoRegistrado":36500,
              "balanza":{"codigo":"balanza-r5-km30","sentido":"HACIA_MONTEVIDEO"},
              "peaje":{"codigo":"R5-30","nombre":"Peaje Ruta 5 km 30","ruta":"5","km":30,"latitud":-34.66,"longitud":-56.22}}]
            """;

    private final BalanzasClient client = new BalanzasClient();
    private HttpServer nodo;
    private int codigo;
    private String autorizacion;
    private String consulta;

    @BeforeEach
    void levantarNodo() throws IOException {
        nodo = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        nodo.createContext("/nodo-balanzas/api/pesajes", exchange -> {
            autorizacion = exchange.getRequestHeaders().getFirst("Authorization");
            consulta = exchange.getRequestURI().getQuery();
            byte[] cuerpo = (codigo == 200 ? RESPUESTA : "{}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(codigo, cuerpo.length);
            try (OutputStream salida = exchange.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        nodo.start();
    }

    @AfterEach
    void bajarNodo() {
        nodo.stop(0);
    }

    @Test
    void devuelveLosPesajesDelNodoConSuOrigen() {
        codigo = 200;

        List<PesajeDto> pesajes = client.getPesajes(nodo(), "clave-central", "SCC9012", LocalDate.of(2026, 10, 22));

        assertEquals("Bearer clave-central", autorizacion);
        assertEquals("matricula=SCC9012&fecha=2026-10-22", consulta);
        assertEquals(1, pesajes.size());
        PesajeDto pesaje = pesajes.get(0);
        assertEquals("balanzas-sur/balanza-r5-km30/1001", pesaje.getIdOrigen());
        assertEquals(7L, pesaje.getIdNodo());
        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 10), pesaje.getFechaHora());
        assertEquals(36500, pesaje.getPesoRegistrado());
        assertEquals("HACIA_MONTEVIDEO", pesaje.getSentido());
        assertEquals("Peaje Ruta 5 km 30", pesaje.getNombrePeaje());
        assertEquals(-56.22, pesaje.getLongitud());
    }

    @Test
    void siElNodoRechazaLaClaveEsUnRechazo() {
        codigo = 401;

        CargaUYException error = assertThrows(CargaUYException.class,
                () -> client.getPesajes(nodo(), "clave-mala", "SCC9012", LocalDate.of(2026, 10, 22)));

        assertEquals(CodigoError.BALANZAS_NODO_RECHAZO, error.getCodigo());
    }

    private NodoPerifericoDto nodo() {
        NodoPerifericoDto dto = new NodoPerifericoDto("balanzas-sur", TipoNodo.BALANZAS,
                "http://localhost:" + nodo.getAddress().getPort() + "/nodo-balanzas");
        dto.setId(7L);
        return dto;
    }
}
