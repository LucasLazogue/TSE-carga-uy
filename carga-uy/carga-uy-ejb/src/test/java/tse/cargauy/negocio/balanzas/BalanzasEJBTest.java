package tse.cargauy.negocio.balanzas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.adaptadores.balanzas.BalanzasClient;
import tse.cargauy.data.nodo.NodoPerifericoDAO;
import tse.cargauy.data.pesaje.PesajeDAO;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.PesajeDto;
import tse.cargauy.dtos.PesajesViajeDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.seguridad.Cifrado;

class BalanzasEJBTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 20, 22, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 10, 21, 3, 0);

    private final BalanzasEJB ejb = new BalanzasEJB();
    private final ExecutorService ejecutor = Executors.newFixedThreadPool(2);
    private final List<NodoPerifericoDto> nodos = new ArrayList<>();
    private final Map<String, List<PesajeDto>> respuestas = new HashMap<>();
    private final List<String> consultas = new ArrayList<>();
    private final Map<String, PesajeDto> guardados = new HashMap<>();
    private String yaGuardadoPorOtraInstancia;

    @BeforeEach
    void iniciar() {
        ejb.ejecutor = ejecutor;
        ejb.cifrado = new Cifrado() {
            @Override
            public String descifrar(String cifrado, String asociado) {
                return "clave-" + asociado;
            }
        };
        ejb.nodoDAO = new NodoPerifericoDAO() {
            @Override
            public List<NodoPerifericoDto> getHabilitados(TipoNodo tipo) {
                return nodos;
            }

            @Override
            public String getClaveCifrada(Long id) {
                return "cifrada";
            }
        };
        ejb.client = new BalanzasClient() {
            @Override
            public List<PesajeDto> getPesajes(NodoPerifericoDto nodo, String clave, String matricula, LocalDate fecha) {
                synchronized (consultas) {
                    consultas.add(nodo.getIdentificador() + " " + clave + " " + matricula + " " + fecha);
                }
                if (!respuestas.containsKey(nodo.getIdentificador())) {
                    throw new CargaUYException(CodigoError.BALANZAS_NO_DISPONIBLE);
                }
                return respuestas.get(nodo.getIdentificador()).stream()
                        .filter(p -> p.getFechaHora().toLocalDate().equals(fecha))
                        .toList();
            }
        };
        ejb.pesajeDAO = new PesajeDAO() {
            @Override
            public boolean existe(String idOrigen) {
                return guardados.containsKey(idOrigen);
            }

            @Override
            public void registrar(PesajeDto pesaje) {
                if (pesaje.getIdOrigen().equals(yaGuardadoPorOtraInstancia)) {
                    throw new CargaUYException(CodigoError.PESAJE_YA_REGISTRADO, pesaje.getIdOrigen());
                }
                guardados.put(pesaje.getIdOrigen(), pesaje);
            }
        };
    }

    @AfterEach
    void cerrar() {
        ejecutor.shutdownNow();
    }

    @Test
    void preguntaACadaNodoPorCadaFechaDelViaje() {
        nodo(1L, "balanzas-sur");
        nodo(2L, "balanzas-norte");
        respuestas.put("balanzas-sur", List.of());
        respuestas.put("balanzas-norte", List.of());

        ejb.getPesajesDelViaje(viaje());

        assertEquals(4, consultas.size());
        assertTrue(consultas.contains("balanzas-sur clave-balanzas-sur SCC9012 2026-10-20"));
        assertTrue(consultas.contains("balanzas-sur clave-balanzas-sur SCC9012 2026-10-21"));
        assertTrue(consultas.contains("balanzas-norte clave-balanzas-norte SCC9012 2026-10-20"));
        assertTrue(consultas.contains("balanzas-norte clave-balanzas-norte SCC9012 2026-10-21"));
    }

    @Test
    void devuelveLosPesajesDelHorarioDelViajeYGuardaTodos() {
        nodo(1L, "balanzas-sur");
        respuestas.put("balanzas-sur", List.of(
                pesaje("balanzas-sur/b1/1", LocalDateTime.of(2026, 10, 20, 23, 30)),
                pesaje("balanzas-sur/b1/2", LocalDateTime.of(2026, 10, 20, 9, 0))));

        PesajesViajeDto resultado = ejb.getPesajesDelViaje(viaje());

        assertTrue(resultado.isCompleto());
        assertEquals(1, resultado.getPesajes().size());
        assertEquals("balanzas-sur/b1/1", resultado.getPesajes().get(0).getIdOrigen());
        assertEquals(87L, guardados.get("balanzas-sur/b1/1").getIdViaje());
        assertEquals(3L, guardados.get("balanzas-sur/b1/1").getIdVehiculo());
        assertNull(guardados.get("balanzas-sur/b1/2").getIdViaje());
    }

    @Test
    void siUnNodoNoRespondeLosDemasSeUsanYQuedaIncompleto() {
        nodo(1L, "balanzas-sur");
        nodo(2L, "balanzas-norte");
        respuestas.put("balanzas-sur", List.of(pesaje("balanzas-sur/b1/1", LocalDateTime.of(2026, 10, 21, 1, 0))));

        PesajesViajeDto resultado = ejb.getPesajesDelViaje(viaje());

        assertFalse(resultado.isCompleto());
        assertEquals(List.of("balanzas-norte"), resultado.getNodosSinResponder());
        assertEquals(1, resultado.getPesajes().size());
    }

    @Test
    void unPesajeQueOtraInstanciaYaGuardoNoEsUnError() {
        nodo(1L, "balanzas-sur");
        respuestas.put("balanzas-sur", List.of(
                pesaje("balanzas-sur/b1/1", LocalDateTime.of(2026, 10, 20, 23, 30)),
                pesaje("balanzas-sur/b1/2", LocalDateTime.of(2026, 10, 21, 0, 30))));
        yaGuardadoPorOtraInstancia = "balanzas-sur/b1/1";

        PesajesViajeDto resultado = ejb.getPesajesDelViaje(viaje());

        assertEquals(2, resultado.getPesajes().size());
        assertTrue(guardados.containsKey("balanzas-sur/b1/2"));
    }

    private void nodo(Long id, String identificador) {
        NodoPerifericoDto nodo = new NodoPerifericoDto(identificador, TipoNodo.BALANZAS, "http://" + identificador);
        nodo.setId(id);
        nodos.add(nodo);
    }

    private static ViajeDto viaje() {
        ViajeDto viaje = new ViajeDto();
        viaje.setId(87L);
        viaje.setIdVehiculo(3L);
        viaje.setMatricula("SCC9012");
        viaje.setFechaInicio(INICIO);
        viaje.setFechaFin(FIN);
        return viaje;
    }

    private static PesajeDto pesaje(String idOrigen, LocalDateTime fechaHora) {
        PesajeDto pesaje = new PesajeDto();
        pesaje.setIdOrigen(idOrigen);
        pesaje.setFechaHora(fechaHora);
        pesaje.setPesoRegistrado(36500);
        return pesaje;
    }
}
