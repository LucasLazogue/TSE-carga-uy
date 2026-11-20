package tse.cargauy.data.posicion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;

import tse.cargauy.data.Contenedores;
import tse.cargauy.dtos.EventoPosicionDto;

// el almacen de posiciones (DA07) contra un MongoDB real: el indice unico por nodo y evento es lo que hace idempotente
// la ingesta de tracking (R6)
class AlmacenPosicionesTest {

    private static MongoDBContainer mongo;
    private static MongoClient cliente;

    private final AlmacenPosiciones almacen = new AlmacenPosiciones();
    private final EventoPosicionDAO posiciones = new EventoPosicionDAO();

    @BeforeAll
    static void arrancar() {
        Contenedores.exigirDocker();
        mongo = new MongoDBContainer(Contenedores.MONGO);
        mongo.start();
        cliente = MongoClients.create(mongo.getConnectionString());
    }

    @AfterAll
    static void cerrar() {
        if (cliente != null) {
            cliente.close();
        }
    }

    // cada test con su base vacia; el almacen la usa como si la hubiera abierto al arrancar
    @BeforeEach
    void preparar() throws ReflectiveOperationException {
        MongoCollection<Document> coleccion = cliente.getDatabase("prueba").getCollection(AlmacenPosiciones.COLECCION);
        coleccion.drop();
        poner("cliente", cliente);
        poner("posiciones", coleccion);
        posiciones.almacen = almacen;
    }

    private void poner(String campo, Object valor) throws ReflectiveOperationException {
        Field f = AlmacenPosiciones.class.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(almacen, valor);
    }

    private static EventoPosicionDto posicion(String idEvento, String matricula, Instant generada) {
        EventoPosicionDto posicion = new EventoPosicionDto();
        posicion.setIdEvento(idEvento);
        posicion.setNodo("tracking-1");
        posicion.setIdNodo(1L);
        posicion.setMatricula(matricula);
        posicion.setLatitud(-34.9);
        posicion.setLongitud(-56.1);
        posicion.setTimestampGeneracion(generada);
        posicion.setTimestampRecepcion(generada.plusSeconds(2));
        return posicion;
    }

    @Test
    void creaLosIndicesAntesDeLaPrimeraEscritura() {
        almacen.posiciones();

        Set<String> indices = StreamSupport.stream(almacen.posiciones().listIndexes().spliterator(), false)
                .map(i -> i.getString("name")).collect(Collectors.toSet());
        assertTrue(indices.containsAll(Set.of("uk_posicion_origen", "ix_posicion_matricula", "ix_posicion_viaje")), indices.toString());
        assertTrue(almacen.conectado());
    }

    // el mismo evento reenviado por el mismo nodo no entra dos veces, y no es un error
    @Test
    void unDuplicadoNoSeGuarda() {
        Instant t = Instant.parse("2026-11-10T12:00:00Z");

        assertTrue(posiciones.registrar(posicion("e-1", "STA1234", t)));
        assertFalse(posiciones.registrar(posicion("e-1", "STA1234", t)));

        assertEquals(1, almacen.posiciones().countDocuments());
        assertEquals("STA1234", posiciones.get(1L, "e-1").getMatricula());
        assertNull(posiciones.get(1L, "e-99"));
    }

    @Test
    void marcaProcesada() {
        posiciones.registrar(posicion("e-1", "STA1234", Instant.parse("2026-11-10T12:00:00Z")));

        posiciones.marcarProcesada(1L, "e-1");

        assertTrue(almacen.posiciones().find(new Document("idEvento", "e-1")).first().getBoolean("procesada"));
    }

    // las posiciones llegan desordenadas; la ruta sale por hora de generacion, y la del viaje solo con las de su tramo
    @Test
    void rutaOrdenadaYAsociadaAlViaje() {
        Instant base = Instant.parse("2026-11-10T12:00:00Z");
        posiciones.registrar(posicion("e-3", "STA1234", base.plusSeconds(60)));
        posiciones.registrar(posicion("e-1", "STA1234", base.minusSeconds(3600)));
        posiciones.registrar(posicion("e-2", "STA1234", base));
        posiciones.registrar(posicion("e-x", "SBB5678", base));

        List<String> ruta = posiciones.getRuta("STA1234").stream().map(EventoPosicionDto::getIdEvento).toList();
        assertEquals(List.of("e-1", "e-2", "e-3"), ruta);

        posiciones.asociarAViaje("STA1234", 3L, base, null, 7L);
        assertEquals(List.of("e-2", "e-3"), posiciones.getRutaDelViaje(7L).stream().map(EventoPosicionDto::getIdEvento).toList());
        assertEquals(3L, posiciones.get(1L, "e-2").getIdVehiculo());

        posiciones.asociarAViaje("STA1234", 3L, base.minusSeconds(7200), base, 8L);
        assertEquals(List.of("e-1"), posiciones.getRutaDelViaje(8L).stream().map(EventoPosicionDto::getIdEvento).toList());
        assertTrue(posiciones.getRutaDelViaje(99L).isEmpty());
    }
}
