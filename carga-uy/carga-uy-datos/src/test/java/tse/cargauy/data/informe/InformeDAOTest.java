package tse.cargauy.data.informe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import tse.cargauy.dtos.FiltroInforme;
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
    void losViajesPublicosFiltranPorRubroYTipoDeCarga() {
        dao.contarViajesPorMesPublico(new FiltroInforme(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 2L, 4L), 5);

        assertTrue(jpql.contains("v.guia.tipoCarga.id = :idTipoCarga AND v.guia.rubro.id = :idRubro GROUP BY"));
        assertEquals(Map.of("umbral", 5L, "finalizado", EstadoViaje.FINALIZADO, "desde", LocalDate.of(2026, 3, 1).atStartOfDay(),
                "hasta", LocalDate.of(2026, 4, 1).atStartOfDay(), "idTipoCarga", 2L, "idRubro", 4L), parametros);
    }

    @Test
    void lasFilasBajoElUmbralVuelvenSinValor() {
        dao.contarViajesPorMesPublico(new FiltroInforme(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null, null), 5);
        String viajes = jpql;
        dao.sumarVolumenPorRubroPublico(new FiltroInforme(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null, null), 5);

        String condicion = "CASE WHEN COUNT(DISTINCT v.guia.empresa.id) >= :umbral AND COUNT(DISTINCT v.vehiculo.id) >= :umbral";
        assertTrue(viajes.contains(condicion + " THEN COUNT(v) ELSE NULL END"));
        assertTrue(jpql.contains(condicion + " THEN SUM(v.guia.volumen) ELSE NULL END"));
    }

    @Test
    void elVolumenPublicoSeOrdenaPorRubroYNoPorValor() {
        dao.sumarVolumenPorRubroPublico(new FiltroInforme(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null, null), 5);

        assertTrue(jpql.endsWith("ORDER BY v.guia.rubro.nombre"));
        assertFalse(jpql.contains(":idRubro"));
        assertFalse(jpql.contains(":idTipoCarga"));
    }

    @Test
    void usaElMismoCriterioQueElTablero() {
        dao.isEmpresaHabilitada(7L, HOY);
        String habilitada = jpql.substring(jpql.indexOf("p.estado"));

        dao.contarEmpresasHabilitadas(HOY);

        assertTrue(jpql.endsWith(habilitada));
    }
}
