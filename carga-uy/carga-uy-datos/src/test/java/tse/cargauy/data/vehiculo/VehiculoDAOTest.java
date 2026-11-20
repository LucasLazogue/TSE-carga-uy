package tse.cargauy.data.vehiculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import tse.cargauy.dtos.FiltroVehiculos;
import tse.cargauy.dtos.Paginacion;

class VehiculoDAOTest {

    private final VehiculoDAO dao = new VehiculoDAO();
    private final Map<String, Object> parametros = new HashMap<>();
    private final List<String> consultas = new ArrayList<>();

    @BeforeEach
    void preparar() throws Exception {
        TypedQuery<?> query = (TypedQuery<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { TypedQuery.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "setParameter" -> {
                        parametros.put((String) args[0], args[1]);
                        yield proxy;
                    }
                    case "getSingleResult" -> 0L;
                    case "getResultList" -> List.of();
                    default -> proxy;
                });
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { EntityManager.class }, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("createQuery")) {
                        consultas.add((String) args[0]);
                        return query;
                    }
                    return null;
                });
        Field campo = VehiculoDAO.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(dao, entityManager);
    }

    @Test
    void laListaYElTotalFiltranPorCapacidad() {
        dao.getByEmpresa(new FiltroVehiculos(7L, 12000), new Paginacion(0, 20));

        assertEquals(2, consultas.size());
        consultas.forEach(jpql -> assertTrue(jpql.contains("v.capacidadCarga >= :capacidadMinima")));
        assertEquals(Map.of("idEmpresa", 7L, "capacidadMinima", 12000), parametros);
    }

    @Test
    void sinCapacidadMinimaPasanTodos() {
        dao.getByEmpresa(new FiltroVehiculos(7L, null), new Paginacion(0, 20));

        assertEquals(0, parametros.get("capacidadMinima"));
    }
}
