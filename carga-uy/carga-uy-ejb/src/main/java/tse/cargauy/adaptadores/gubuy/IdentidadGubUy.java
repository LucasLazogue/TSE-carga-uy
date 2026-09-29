package tse.cargauy.adaptadores.gubuy;

public class IdentidadGubUy {

    private final String cedula;
    private final String correo;

    public IdentidadGubUy(String cedula, String correo) {
        this.cedula = cedula;
        this.correo = correo;
    }

    public String getCedula() {
        return cedula;
    }

    public String getCorreo() {
        return correo;
    }
}
