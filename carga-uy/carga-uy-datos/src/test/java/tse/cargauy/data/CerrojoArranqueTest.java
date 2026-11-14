package tse.cargauy.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import tse.cargauy.data.fiscalizacion.InstaladorAuditoria;
import tse.cargauy.data.testdata.DatosPrueba;

// sin base: un EntityManager falso que anota las sentencias nativas en el orden en que se ejecutan
class CerrojoArranqueTest {

    private final List<String> sentencias = new ArrayList<>();

    @Test
    void tomaElCerrojoDeArranqueDePostgres() {
        CerrojoArranque.tomar(entityManager());

        assertEquals(List.of(CerrojoArranque.SENTENCIA), sentencias);
    }

    @Test
    void elInstaladorDeAuditoriaTomaElCerrojoAntesDeCrearLosTriggers() throws Exception {
        InstaladorAuditoria instalador = new InstaladorAuditoria();
        Field campo = InstaladorAuditoria.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(instalador, entityManager());

        instalador.init();

        assertEquals(CerrojoArranque.SENTENCIA, sentencias.get(0));
        assertEquals(4, sentencias.size());
    }

    @Test
    void losDatosDePruebaTomanElCerrojoAntesDeMirarLaBase() throws Exception {
        DatosPrueba datos = new DatosPrueba();
        Field campo = DatosPrueba.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(datos, entityManager());

        // sin los DAOs falla en la primera consulta: para entonces el cerrojo ya tiene que estar tomado
        assertThrows(NullPointerException.class, datos::init);

        assertEquals(List.of(CerrojoArranque.SENTENCIA), sentencias);
    }

    private EntityManager entityManager() {
        return (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { EntityManager.class },
                (proxy, metodo, args) -> {
                    if (!metodo.getName().equals("createNativeQuery")) {
                        throw new UnsupportedOperationException(metodo.getName());
                    }
                    sentencias.add((String) args[0]);
                    return consulta();
                });
    }

    private Query consulta() {
        return (Query) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Query.class },
                (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "getSingleResult" -> 1;
                    case "executeUpdate" -> 0;
                    default -> throw new UnsupportedOperationException(metodo.getName());
                });
    }
}
