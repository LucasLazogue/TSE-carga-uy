package tse.cargauy.data.testdata;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// DAOs falsos en memoria para los tests de los datos de prueba
final class DaosFalsos {

    private DaosFalsos() {
    }

    static <T> List<T> lista(int cantidad, Function<Integer, T> elemento) {
        List<T> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            lista.add(elemento.apply(i));
        }
        return lista;
    }

    static <T> T conId(T dto, long id) {
        try {
            dto.getClass().getMethod("setId", Long.class).invoke(dto, id);
            return dto;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // implementa la interfaz con los metodos dados; cualquier otro falla, asi el test muestra si se llamo algo de mas
    static <T> T falso(Class<T> tipo, Map<String, Function<Object[], Object>> metodos) {
        return tipo.cast(Proxy.newProxyInstance(DaosFalsos.class.getClassLoader(), new Class<?>[] { tipo },
                (proxy, metodo, args) -> {
                    Function<Object[], Object> implementacion = metodos.get(metodo.getName());
                    if (implementacion == null) {
                        throw new UnsupportedOperationException(tipo.getSimpleName() + "." + metodo.getName());
                    }
                    Object resultado = implementacion.apply(args);
                    return metodo.getReturnType() == void.class ? null : resultado;
                }));
    }
}
