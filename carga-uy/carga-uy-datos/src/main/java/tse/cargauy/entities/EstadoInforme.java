package tse.cargauy.entities;

public enum EstadoInforme {

    PENDIENTE("Generándose"),
    GENERADO("Disponible"),
    FALLIDO("Falló"),
    VENCIDO("Vencido");

    private final String nombre;

    EstadoInforme(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
