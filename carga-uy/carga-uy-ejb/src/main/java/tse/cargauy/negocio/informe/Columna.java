package tse.cargauy.negocio.informe;

import java.util.Objects;
import java.util.function.Function;

public final class Columna<T> {

    private final String clave;
    private final Function<T, Object> valor;

    public Columna(String clave, Function<T, Object> valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public String getTitulo() {
        return TextosInforme.resolver(clave);
    }

    public String valor(T fila) {
        return Objects.toString(valor.apply(fila), "");
    }
}
