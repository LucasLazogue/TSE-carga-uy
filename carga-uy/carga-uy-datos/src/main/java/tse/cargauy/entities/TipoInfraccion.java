package tse.cargauy.entities;

public enum TipoInfraccion {

    EXCESO_CARGA("Exceso de carga"),
    SIN_HABILITACION("Sin habilitación"),
    VIAJE_SIN_GUIA("Viaje sin guía"),
    DESVIO_RUTA("Desvío de ruta");

    private final String nombre;

    TipoInfraccion(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
