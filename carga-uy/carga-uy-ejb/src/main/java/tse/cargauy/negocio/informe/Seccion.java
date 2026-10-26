package tse.cargauy.negocio.informe;

public final class Seccion {

    private final String clave;
    private final Tabla<?> tabla;

    public Seccion(String clave, Tabla<?> tabla) {
        this.clave = clave;
        this.tabla = tabla;
    }

    public String getTitulo() {
        return TextosInforme.resolver(clave);
    }

    public Tabla<?> getTabla() {
        return tabla;
    }
}
