package tse.cargauy.entities;

public enum TipoInforme {

    HABILITADOS("Empresas y vehículos habilitados"),
    VIAJES_POR_PERIODO("Viajes realizados por período"),
    VOLUMEN_POR_RUBRO("Volumen transportado por rubro"),
    INFRACCIONES_POR_TIPO("Infracciones por tipo"),
    TASA_CONFIRMACION("Tasa de infracciones confirmadas");

    private final String nombre;

    TipoInforme(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
