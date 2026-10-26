package tse.cargauy.negocio.informe;

import java.util.List;

public final class Tabla<T> {

    private final List<T> filas;
    private final List<Columna<T>> columnas;

    public Tabla(List<T> filas, List<Columna<T>> columnas) {
        this.filas = filas;
        this.columnas = columnas;
    }

    public List<String> getEncabezados() {
        return columnas.stream().map(Columna::getTitulo).toList();
    }

    public List<String[]> getValores() {
        return filas.stream().map(fila -> columnas.stream().map(c -> c.valor(fila)).toArray(String[]::new)).toList();
    }

    public boolean isVacia() {
        return filas.isEmpty();
    }
}
