package tse.cargauy.negocio.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.nodo.NodoPerifericoDAOLocal;
import tse.cargauy.data.testdata.DatosPrueba;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.seguridad.Cifrado;

// con DAOs falsos en memoria: lo que se prueba es que elenco queda y que arrancar de nuevo no duplica nada
class ElencoDemoTest {

    private static final Long EMPRESA = 10L;

    private final Map<String, String> entorno = new HashMap<>();
    private final List<String> llamadas = new ArrayList<>();

    // estado de la base falsa
    private final Map<String, UsuarioDto> ciudadanos = new HashMap<>();
    private final List<String> funcionarios = new ArrayList<>();
    private final List<String> responsables = new ArrayList<>();
    private final List<String> choferes = new ArrayList<>();
    private final List<ViajeDto> viajes = new ArrayList<>();
    private final Map<String, NodoPerifericoDto> nodos = new HashMap<>();
    private final Map<String, String> clavesCifradas = new HashMap<>();
    private int datosPrueba;

    private ElencoDemo elenco;

    @BeforeEach
    void preparar() {
        elenco = new ElencoDemo() {
            @Override
            String entorno(String variable) {
                return entorno.get(variable);
            }
        };
        elenco.entityManager = entityManager();
        elenco.datosPrueba = new DatosPrueba() {
            @Override
            public void init() {
                datosPrueba++;
            }
        };
        elenco.cifrado = new Cifrado() {
            @Override
            public String cifrar(String texto, String asociado) {
                return "cifrada(" + texto + "," + asociado + ")";
            }
        };
        elenco.usuarioDAO = falso(UsuarioDAOLocal.class, Map.of(
                "getFuncionarioByCedula", a -> funcionarios.contains(a[0]) ? new UsuarioDto() : null,
                "addFuncionario", a -> funcionarios.add((String) a[0]),
                "getCiudadanoByCedula", a -> ciudadanos.get(a[0]),
                "addCiudadano", a -> nuevoCiudadano((String) a[0], (String) a[1]),
                "getResponsableVigente", a -> responsables.contains(a[0] + "@" + a[1]) ? 1L : null,
                "addResponsable", a -> responsables.add(a[0] + "@" + a[1]),
                "getChoferVigente", a -> choferes.contains(a[0] + "@" + a[1]) ? 1L : null,
                "addChofer", a -> {
                    choferes.add(a[0] + "@" + a[1]);
                    return null;
                }));
        elenco.empresaDAO = falso(EmpresaDAOLocal.class, Map.of(
                "getEmpresaByNro", a -> (Integer) a[0] == 1001 ? empresa() : null));
        elenco.vehiculoDAO = falso(VehiculoDAOLocal.class, Map.of(
                "getVehiculoByMatricula", a -> vehiculo((String) a[0])));
        elenco.guiaDAO = falso(GuiaDAOLocal.class, Map.of(
                "getRubros", a -> List.of(conId(new RubroDto("Agropecuario"))),
                "getTiposCarga", a -> List.of(conId(new TipoCargaDto("Carga general"))),
                "addGuia", a -> conId((GuiaDto) a[0])));
        elenco.viajeDAO = falso(ViajeDAOLocal.class, Map.of(
                "getActivosDelChofer", a -> viajes.stream().filter(v -> v.getIdChofer().equals(a[0])).toList(),
                "addViaje", a -> agregarViaje((ViajeDto) a[0])));
        elenco.nodoDAO = falso(NodoPerifericoDAOLocal.class, Map.of(
                "getByIdentificador", a -> nodos.get(a[0]),
                "addNodo", a -> agregarNodo((NodoPerifericoDto) a[0], (String) a[1])));
    }

    @Test
    void sinVariablesNoHaceNada() {
        elenco.iniciar();

        assertTrue(llamadas.isEmpty());
        assertEquals(0, datosPrueba);
    }

    @Test
    void dejaElElencoYLosNodosHabilitados() {
        entorno.put("DEMO_FUNCIONARIOS", "51111111");
        entorno.put("DEMO_RESPONSABLES", "52222222");
        entorno.put("DEMO_CHOFERES", " 53333333 ,");
        entorno.put("DEMO_CIUDADANOS", "54444444");
        entorno.put("DEMO_NODOS", "tracking-1; balanzas-sur|https://balanzas.example/nodo-balanzas|secreta");

        elenco.iniciar();

        assertEquals(1, datosPrueba);
        assertEquals(List.of("51111111"), funcionarios);
        Long responsable = ciudadanos.get("52222222").getId();
        Long chofer = ciudadanos.get("53333333").getId();
        assertEquals(List.of(responsable + "@" + EMPRESA), responsables);
        assertEquals(List.of(chofer + "@" + EMPRESA), choferes);
        assertTrue(ciudadanos.containsKey("54444444"));
        // el correo lo completa gub.uy en el primer ingreso
        assertNull(ciudadanos.get("53333333").getCorreo());

        assertEquals(1, viajes.size());
        assertEquals(chofer, viajes.get(0).getIdChofer());

        assertEquals(EstadoNodo.HABILITADO, nodos.get("tracking-1").getEstado());
        assertEquals(TipoNodo.TRACKING, nodos.get("tracking-1").getTipo());
        assertNull(clavesCifradas.get("tracking-1"));
        assertEquals(EstadoNodo.HABILITADO, nodos.get("balanzas-sur").getEstado());
        assertEquals("https://balanzas.example/nodo-balanzas", nodos.get("balanzas-sur").getPuntoAcceso());
        assertEquals("cifrada(secreta,balanzas-sur)", clavesCifradas.get("balanzas-sur"));
    }

    @Test
    void arrancarDeNuevoNoDuplicaNada() {
        entorno.put("DEMO_FUNCIONARIOS", "51111111");
        entorno.put("DEMO_RESPONSABLES", "52222222");
        entorno.put("DEMO_CHOFERES", "53333333");
        entorno.put("DEMO_NODOS", "tracking-1");

        elenco.iniciar();
        elenco.iniciar();

        assertEquals(1, funcionarios.size());
        assertEquals(1, responsables.size());
        assertEquals(1, choferes.size());
        assertEquals(1, viajes.size());
        assertEquals(1, nodos.size());
    }

    @Test
    void sinResponsableNoCreaElViajeDeRespaldo() {
        entorno.put("DEMO_CHOFERES", "53333333");

        elenco.iniciar();

        assertEquals(1, choferes.size());
        assertTrue(viajes.isEmpty());
    }

    @Test
    void leeLasCedulasYLosNodos() {
        assertEquals(List.of(), ElencoDemo.cedulas(null));
        assertEquals(List.of("1", "2"), ElencoDemo.cedulas(" 1, ,2 "));
        assertEquals(List.of(), ElencoDemo.nodos(" ; "));
        assertEquals(1001, ElencoDemo.numero(null, 1001));
        assertEquals(1002, ElencoDemo.numero(" 1002 ", 1001));
        assertThrows(IllegalStateException.class, () -> ElencoDemo.nodos("balanzas-sur|https://x"));
    }

    private UsuarioDto nuevoCiudadano(String cedula, String correo) {
        UsuarioDto ciudadano = new UsuarioDto();
        ciudadano.setId((long) (ciudadanos.size() + 100));
        ciudadano.setCorreo(correo);
        ciudadanos.put(cedula, ciudadano);
        return ciudadano;
    }

    private ViajeDto agregarViaje(ViajeDto viaje) {
        viajes.add(viaje);
        return viaje;
    }

    private Object agregarNodo(NodoPerifericoDto nodo, String claveCifrada) {
        nodos.put(nodo.getIdentificador(), nodo);
        clavesCifradas.put(nodo.getIdentificador(), claveCifrada);
        return null;
    }

    private static EmpresaDto empresa() {
        EmpresaDto empresa = new EmpresaDto();
        empresa.setId(EMPRESA);
        return empresa;
    }

    private static VehiculoDto vehiculo(String matricula) {
        VehiculoDto vehiculo = new VehiculoDto(matricula, "Scania", "R 450", 9000, 26000, EMPRESA);
        vehiculo.setId(7L);
        return vehiculo;
    }

    private static <T> T conId(T dto) {
        try {
            dto.getClass().getMethod("setId", Long.class).invoke(dto, 1L);
            return dto;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // implementa la interfaz con los metodos dados; cualquier otro falla, asi el test muestra si se llamo algo de mas
    private <T> T falso(Class<T> tipo, Map<String, Function<Object[], Object>> metodos) {
        return tipo.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { tipo },
                (proxy, metodo, args) -> {
                    Function<Object[], Object> implementacion = metodos.get(metodo.getName());
                    if (implementacion == null) {
                        throw new UnsupportedOperationException(tipo.getSimpleName() + "." + metodo.getName());
                    }
                    llamadas.add(metodo.getName());
                    Object resultado = implementacion.apply(args);
                    return metodo.getReturnType() == void.class ? null : resultado;
                }));
    }

    private EntityManager entityManager() {
        Query consulta = (Query) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Query.class },
                (proxy, metodo, args) -> 1);
        return (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { EntityManager.class },
                (proxy, metodo, args) -> {
                    llamadas.add(metodo.getName());
                    return consulta;
                });
    }
}
