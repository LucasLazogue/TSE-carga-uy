package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class HabilitacionEmpresaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombrePublico;
    private String razonSocial;
    private boolean habilitada;
    private LocalDate fecha;

    public HabilitacionEmpresaDto() {
    }

    public HabilitacionEmpresaDto(String nombrePublico, String razonSocial, boolean habilitada, LocalDate fecha) {
        this.nombrePublico = nombrePublico;
        this.razonSocial = razonSocial;
        this.habilitada = habilitada;
        this.fecha = fecha;
    }

    public String getNombrePublico() {
        return nombrePublico;
    }

    public void setNombrePublico(String nombrePublico) {
        this.nombrePublico = nombrePublico;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    public void setHabilitada(boolean habilitada) {
        this.habilitada = habilitada;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}
