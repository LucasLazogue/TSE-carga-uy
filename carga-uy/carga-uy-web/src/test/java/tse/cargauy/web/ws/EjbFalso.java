package tse.cargauy.web.ws;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// EJB falso para los recursos REST: registra cada llamada con sus argumentos y responde lo configurado por metodo.
// Los recursos solo traducen HTTP a llamadas al EJB, asi que los tests miran que llamada llego y con que datos
public final class EjbFalso<T> {

    private final T ejb;
    private final List<String> metodos = new ArrayList<>();
    private final List<Object[]> argumentos = new ArrayList<>();
    private final Map<String, Function<Object[], Object>> respuestas = new HashMap<>();

    private EjbFalso(Class<T> tipo) {
        ejb = tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] { tipo }, (proxy, metodo, args) -> {
            if (metodo.getDeclaringClass() == Object.class) {
                return metodo.getName().equals("toString") ? "EjbFalso[" + tipo.getSimpleName() + "]" : null;
            }
            Object[] recibidos = args == null ? new Object[0] : args;
            metodos.add(metodo.getName());
            argumentos.add(recibidos);
            Function<Object[], Object> respuesta = respuestas.get(metodo.getName());
            return respuesta == null ? porDefecto(metodo) : respuesta.apply(recibidos);
        }));
    }

    public static <T> EjbFalso<T> de(Class<T> tipo) {
        return new EjbFalso<>(tipo);
    }

    public T ejb() {
        return ejb;
    }

    public EjbFalso<T> responde(String metodo, Object valor) {
        respuestas.put(metodo, args -> valor);
        return this;
    }

    public EjbFalso<T> responde(String metodo, Function<Object[], Object> respuesta) {
        respuestas.put(metodo, respuesta);
        return this;
    }

    public EjbFalso<T> falla(String metodo, RuntimeException error) {
        respuestas.put(metodo, args -> {
            throw error;
        });
        return this;
    }

    // los metodos llamados, en orden
    public List<String> llamadas() {
        return metodos;
    }

    // los argumentos de la ultima llamada al metodo
    public Object[] argumentos(String metodo) {
        for (int i = metodos.size() - 1; i >= 0; i--) {
            if (metodos.get(i).equals(metodo)) {
                return argumentos.get(i);
            }
        }
        throw new AssertionError("no se llamo a " + metodo + "; llamadas: " + metodos);
    }

    public Object argumento(String metodo, int posicion) {
        return argumentos(metodo)[posicion];
    }

    @Override
    public String toString() {
        return metodos + " " + argumentos.stream().map(Arrays::toString).toList();
    }

    private static Object porDefecto(Method metodo) {
        Class<?> tipo = metodo.getReturnType();
        if (tipo == boolean.class) {
            return false;
        }
        if (tipo == long.class) {
            return 0L;
        }
        if (tipo == int.class) {
            return 0;
        }
        return null;
    }
}
