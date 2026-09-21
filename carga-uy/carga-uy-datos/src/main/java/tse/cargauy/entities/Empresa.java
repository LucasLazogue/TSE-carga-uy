package tse.cargauy.entities;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Empresa {

    @Id @GeneratedValue
    private Long id;
    @Column(unique = true, nullable = false)
    private int nroEmpresa;
    private String nombrePublico;
    private String razonSocial;
    private String direccionPrincipal;
    private LocalDate fechaAlta;
    @OneToMany(mappedBy = "empresa", cascade = CascadeType.REMOVE)
    private List<Vehiculo> vehiculos;

    public Empresa() {
    }

    public Empresa(int nroEmpresa, String nombrePublico, String razonSocial, String direccionPrincipal, LocalDate fechaAlta) {
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

    public List<Vehiculo> getVehiculos() {
        return vehiculos;
    }

    public void setVehiculos(List<Vehiculo> vehiculos) {
        this.vehiculos = vehiculos;
    }

}
