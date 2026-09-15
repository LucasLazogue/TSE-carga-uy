package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class EmpresaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private int nroEmpresa;
    private String nombrePublico;
    private String razonSocial;
    private String direccionPrincipal;
    private LocalDate fechaAlta;

    public EmpresaDto() {
    }

    public EmpresaDto(int nroEmpresa, String nombrePublico, String razonSocial, String direccionPrincipal, LocalDate fechaAlta) {
        this.nroEmpresa = nroEmpresa;
        this.nombrePublico = nombrePublico;
        this.razonSocial = razonSocial;
        this.direccionPrincipal = direccionPrincipal;
        this.fechaAlta = fechaAlta;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getNroEmpresa() {
        return nroEmpresa;
    }

    public void setNroEmpresa(int nroEmpresa) {
        this.nroEmpresa = nroEmpresa;
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

    public String getDireccionPrincipal() {
        return direccionPrincipal;
    }

    public void setDireccionPrincipal(String direccionPrincipal) {
        this.direccionPrincipal = direccionPrincipal;
    }

    public LocalDate getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDate fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    @Override
    public String toString() {
        return "Nro: " + nroEmpresa + ", Nombre Público: " + nombrePublico + ", Razón Social: " + razonSocial +
                ", Dirección: " + direccionPrincipal + ", Fecha de Alta: " + fechaAlta;
    }
}
