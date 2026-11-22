package tse.cargauy.data.informe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import tse.cargauy.entities.EstadoPermiso;
import tse.cargauy.entities.EstadoViaje;

class InformeDAOTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 14);

    private final InformeDAO dao = new InformeDAO();
    private final Map<String, Object> parametros = new HashMap<>();
    private String jpql;
    private long resultado;

    @BeforeEach
    void preparar() throws Exception {
        TypedQuery<?> query = (TypedQuery<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { TypedQuery.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "setParameter" -> {
                        parametros.put((String) args[0], args[1]);
                        yield proxy;
                    }
                    case "getSingleResult" -> resultado;
                    case "getResultList" -> List.of();
                    case "getResultStream" -> Stream.empty();
                    default -> proxy;
                });
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { EntityManager.class }, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("createQuery")) {
                        jpql = (String) args[0];
                        return query;
                    }
                    return null;
                });
        Field campo = InformeDAO.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(dao, entityManager);
    }

    @Test
    void laEmpresaEstaHabilitadaConUnVehiculoHabilitado() {
        resultado = 1;

        assertTrue(dao.isEmpresaHabilitada(7L, HOY));
        assertEquals(Map.of("idEmpresa", 7L, "aprobado", EstadoPermiso.APROBADO, "fecha", HOY), parametros);
    }

    @Test
    void sinVehiculosHabilitadosNoLoEsta() {
        resultado = 0;

        assertFalse(dao.isEmpresaHabilitada(7L, HOY));
    }

    @Test
    void elViajeVaAlHistoricoRecienCuandoFinaliza() {
        assertNull(dao.getViajeHistorico(21L));

        assertTrue(jpql.endsWith("FROM Viaje v WHERE v.estado = :finalizado AND v.id = :id"));
        assertEquals(Map.of("finalizado", EstadoViaje.FINALIZADO, "id", 21L), parametros);
    }

    @Test
    void elCasoSinViajeIgualVaAlHistorico() {
        dao.getCasoHistorico(40L);

        assertTrue(jpql.contains("FROM CasoFiscalizacion c LEFT JOIN c.viaje v LEFT JOIN v.guia g LEFT JOIN g.tipoCarga t"));
        assertTrue(jpql.endsWith("WHERE c.id = :id"));
        assertEquals(Map.of("id", 40L), parametros);
    }

    @Test
    void usaElMismoCriterioQueElTablero() {
        dao.isEmpresaHabilitada(7L, HOY);
        String habilitada = jpql.substring(jpql.indexOf("p.estado"));

        dao.contarEmpresasHabilitadas(HOY);

        assertTrue(jpql.endsWith(habilitada));
    }
}
