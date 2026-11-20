package tse.cargauy.data;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityManager;

// EntityManager sin base para las consultas armadas con Criteria: cada objeto de la API (CriteriaBuilder,
// CriteriaQuery, Root, Path, Fetch, TypedQuery...) es un proxy que anota la llamada y devuelve otro proxy. Las
// consultas devuelven 'resultado' en getResultList y 'total' en getSingleResult. Los atributos del metamodelo
// (Viaje_.guia...) llegan en null: sin Hibernate nadie los carga
public final class CriteriaGrabada {

    public record Llamada(Object objeto, String metodo, List<Object> argumentos, Object resultado) {
    }

    public final List<Llamada> llamadas = new ArrayList<>();
    public List<?> resultado = List.of();
    public long total;

    public EntityManager entityManager() {
        return (EntityManager) proxy(EntityManager.class);
    }

    public List<Llamada> de(String metodo) {
        return llamadas.stream().filter(l -> l.metodo().equals(metodo)).toList();
    }

    private Object proxy(Class<?> tipo) {
        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { tipo }, (proxy, metodo, args) -> {
            if (metodo.getDeclaringClass() == Object.class) {
                return switch (metodo.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> tipo.getSimpleName();
                };
            }
            Class<?> devuelve = metodo.getReturnType();
            Object respuesta;
            if (metodo.getName().equals("getResultList")) {
                respuesta = resultado;
            } else if (metodo.getName().equals("getSingleResult")) {
                respuesta = total;
            } else if (devuelve == boolean.class) {
                respuesta = false;
            } else if (devuelve == int.class) {
                respuesta = 0;
            } else if (devuelve.isInterface()) {
                respuesta = proxy(devuelve);
            } else {
                respuesta = null;
            }
            llamadas.add(new Llamada(proxy, metodo.getName(), args == null ? List.of() : Arrays.asList(args), respuesta));
            return respuesta;
        });
    }
}
