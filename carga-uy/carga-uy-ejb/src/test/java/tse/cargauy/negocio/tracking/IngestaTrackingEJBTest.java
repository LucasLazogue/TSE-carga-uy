package tse.cargauy.negocio.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.util.TypeLiteral;

import tse.cargauy.data.nodo.NodoPerifericoDAO;
import tse.cargauy.data.posicion.EventoPosicionDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.TipoNodo;

class IngestaTrackingEJBTest {

    private final IngestaTrackingEJB ejb = new IngestaTrackingEJB();
    private final Map<String, NodoPerifericoDto> nodos = new HashMap<>();
    // lo que guardaria MongoDB, por los campos del indice unico: nodo + id del evento
    private final Map<String, EventoPosicionDto> porClave = new HashMap<>();
    private final List<EventoPosicionDto> guardados = new ArrayList<>();
    private final List<PosicionProcesada> terminadas = new ArrayList<>();

    @BeforeEach
    void preparar() {
        nodo(3L, "tracking-1", TipoNodo.TRACKING, EstadoNodo.HABILITADO);
        ejb.nodoDAO = new NodoPerifericoDAO() {
            @Override
            public NodoPerifericoDto getByIdentificador(String identificador) {
                return nodos.get(identificador);
            }
        };
        ejb.vehiculoDAO = new VehiculoDAO() {
            @Override
            public VehiculoDto getVehiculoByMatricula(String matricula) {
                if (!"STA1234".equals(matricula)) {
                    return null;
                }
                VehiculoDto vehiculo = new VehiculoDto();
                vehiculo.setId(11L);
                vehiculo.setMatricula(matricula);
                return vehiculo;
            }
        };
        ejb.eventoDAO = new EventoPosicionDAO() {
            @Override
            public boolean registrar(EventoPosicionDto evento) {
                if (porClave.putIfAbsent(clave(evento.getIdNodo(), evento.getIdEvento()), evento) != null) {
                    return false;
                }
                guardados.add(evento);
                return true;
            }

            @Override
            public EventoPosicionDto get(Long idNodo, String idEvento) {
                return porClave.get(clave(idNodo, idEvento));
            }
        };
        ejb.procesadas = new EventoFalso();
    }

    @Test
    void registraLaPosicionDeUnNodoHabilitado() {
        assertEquals(ResultadoIngesta.REGISTRADO, ejb.procesar(evento("ev-1", "tracking-1", "STA1234")));

        EventoPosicionDto guardado = guardados.get(0);
        assertEquals(3L, guardado.getIdNodo());
        assertEquals(11L, guardado.getIdVehiculo());
        assertNotNull(guardado.getTimestampRecepcion());
    }

    @Test
    void alTerminarAvisaQueLaPosicionSePuedeMarcarComoProcesada() {
        ejb.procesar(evento("ev-1", "tracking-1", "STA1234"));

        assertEquals(List.of(new PosicionProcesada(3L, "ev-1")), terminadas);
    }

    @Test
    void unDuplicadoDeUnaPosicionYaProcesadaSeDescarta() {
        ejb.procesar(evento("ev-1", "tracking-1", "STA1234"));
        porClave.get(clave(3L, "ev-1")).setProcesada(true);
        terminadas.clear();

        assertEquals(ResultadoIngesta.DUPLICADO, ejb.procesar(evento("ev-1", "tracking-1", "STA1234")));
        assertEquals(1, guardados.size());
        assertTrue(terminadas.isEmpty());
    }

    // un intento anterior la guardo y fallo despues: el reenvio no la guarda de nuevo, pero termina de procesarla
    @Test
    void unDuplicadoDeUnaPosicionQueQuedoSinProcesarSeCompleta() {
        ejb.procesar(evento("ev-1", "tracking-1", "STA1234"));
        terminadas.clear();

        assertEquals(ResultadoIngesta.COMPLETADO, ejb.procesar(evento("ev-1", "tracking-1", "STA1234")));
        assertEquals(1, guardados.size());
        assertEquals(List.of(new PosicionProcesada(3L, "ev-1")), terminadas);
    }

    // el id del evento lo genera cada nodo: dos nodos pueden repetirlo sin que sea el mismo evento
    @Test
    void elMismoIdDeEventoDeOtroNodoNoEsDuplicado() {
        nodo(4L, "tracking-2", TipoNodo.TRACKING, EstadoNodo.HABILITADO);
        ejb.procesar(evento("ev-1", "tracking-1", "STA1234"));

        assertEquals(ResultadoIngesta.REGISTRADO, ejb.procesar(evento("ev-1", "tracking-2", "STA1234")));
    }

    @Test
    void unaMatriculaDesconocidaSeGuardaSinVehiculo() {
        assertEquals(ResultadoIngesta.REGISTRADO, ejb.procesar(evento("ev-1", "tracking-1", "XXX0000")));

        assertNull(guardados.get(0).getIdVehiculo());
    }

    @Test
    void descartaLosEventosDeUnNodoQueNoEstaRegistrado() {
        assertEquals(ResultadoIngesta.NODO_DESCONOCIDO, ejb.procesar(evento("ev-1", "tracking-9", "STA1234")));
        assertTrue(guardados.isEmpty());
        assertTrue(terminadas.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({ "TRACKING, DESHABILITADO", "TRACKING, BAJA", "BALANZAS, HABILITADO" })
    void descartaLosEventosDeUnNodoQueNoEsDeTrackingHabilitado(TipoNodo tipo, EstadoNodo estado) {
        nodo(5L, "otro", tipo, estado);

        assertEquals(ResultadoIngesta.NODO_NO_HABILITADO, ejb.procesar(evento("ev-1", "otro", "STA1234")));
        assertTrue(guardados.isEmpty());
    }

    private static String clave(Long idNodo, String idEvento) {
        return idNodo + "|" + idEvento;
    }

    private void nodo(Long id, String identificador, TipoNodo tipo, EstadoNodo estado) {
        NodoPerifericoDto nodo = new NodoPerifericoDto(identificador, tipo, "http://nodo");
        nodo.setId(id);
        nodo.setEstado(estado);
        nodos.put(identificador, nodo);
    }

    private static EventoPosicionDto evento(String idEvento, String nodo, String matricula) {
        EventoPosicionDto evento = new EventoPosicionDto();
        evento.setIdEvento(idEvento);
        evento.setNodo(nodo);
        evento.setMatricula(matricula);
        evento.setLatitud(-34.9);
        evento.setLongitud(-56.1);
        evento.setTimestampGeneracion(Instant.parse("2026-10-06T12:00:00Z"));
        return evento;
    }

    // junta lo que la ingesta dispara; en el servidor lo recibe MarcadorPosiciones al confirmarse la transaccion
    private class EventoFalso implements Event<PosicionProcesada> {
        @Override
        public void fire(PosicionProcesada posicion) {
            terminadas.add(posicion);
        }

        @Override
        public <U extends PosicionProcesada> CompletionStage<U> fireAsync(U posicion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends PosicionProcesada> CompletionStage<U> fireAsync(U posicion, NotificationOptions opciones) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Event<PosicionProcesada> select(Annotation... calificadores) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends PosicionProcesada> Event<U> select(Class<U> subtipo, Annotation... calificadores) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends PosicionProcesada> Event<U> select(TypeLiteral<U> subtipo, Annotation... calificadores) {
            throw new UnsupportedOperationException();
        }
    }
}
