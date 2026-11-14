package tse.cargauy.data.empresa;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.entities.Empresa;

class EmpresaDAOTest {

    private final EmpresaDAO dao = new EmpresaDAO();
    private final Map<String, Object> parametros = new HashMap<>();
    private String jpql;
    private Integer maximo;
    private List<Empresa> resultado = List.of();

    @BeforeEach
    void preparar() throws Exception {
        TypedQuery<?> query = (TypedQuery<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { TypedQuery.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "setParameter" -> {
                        parametros.put((String) args[0], args[1]);
                        yield proxy;
                    }
                    case "setMaxResults" -> {
                        maximo = (Integer) args[0];
                        yield proxy;
                    }
                    case "getResultList" -> resultado;
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
        Field campo = EmpresaDAO.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(dao, entityManager);
    }

    @Test
    void laRazonSocialSeComparaExactaSinMayusculasNiEspacios() {
        dao.getByRazonSocial("  Transportes del SUR S.A. ", 2);

        assertEquals("transportes del sur s.a.", parametros.get("razonSocial"));
        assertEquals("SELECT e FROM Empresa e WHERE LOWER(TRIM(e.razonSocial)) = :razonSocial ORDER BY e.nroEmpresa", jpql);
        assertEquals(2, maximo);
    }

    @Test
    void devuelveLasEmpresasEncontradas() {
        resultado = List.of(new Empresa(1001, "Transportes del Sur", "Transportes del Sur S.A.", "Av. Italia 3456",
                LocalDate.of(2020, 1, 1)));

        List<EmpresaDto> empresas = dao.getByRazonSocial("Transportes del Sur S.A.", 2);

        assertEquals(1, empresas.size());
        assertEquals(1001, empresas.get(0).getNroEmpresa());
        assertEquals("Transportes del Sur", empresas.get(0).getNombrePublico());
    }
}
