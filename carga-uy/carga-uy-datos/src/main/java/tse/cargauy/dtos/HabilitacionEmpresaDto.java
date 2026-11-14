package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class HabilitacionEmpresaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String nombrePublico;
    private final String razonSocial;
    private final boolean habilitada;
    private final LocalDate fecha;

    public HabilitacionEmpresaDto(String nombrePublico, String razonSocial, boolean habilitada, LocalDate fecha) {
        this.nombrePublico = nombrePublico;
        this.razonSocial = razonSocial;
        this.habilitada = habilitada;
        this.fecha = fecha;
    }

    public String getNombrePublico() {
        return nombrePublico;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    public LocalDate getFecha() {
        return fecha;
    }
}
