package tse.cargauy.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class VinculoEmpresa {

    @Id @GeneratedValue
    private Long id;
    @ManyToOne(optional = false)
    private Ciudadano ciudadano;
    @ManyToOne(optional = false)
    private Empresa empresa;
    @Column(nullable = false)
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;

    protected VinculoEmpresa() {
    }

    protected VinculoEmpresa(Ciudadano ciudadano, Empresa empresa, LocalDate fechaDesde) {
        this.ciudadano = ciudadano;
        this.empresa = empresa;
        this.fechaDesde = fechaDesde;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ciudadano getCiudadano() {
        return ciudadano;
    }

    public void setCiudadano(Ciudadano ciudadano) {
        this.ciudadano = ciudadano;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

}
