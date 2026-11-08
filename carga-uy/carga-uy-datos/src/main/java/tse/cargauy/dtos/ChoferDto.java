package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

// un vinculo de chofer con la empresa; id es el del ciudadano, que es lo que recibe un viaje
public class ChoferDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long idVinculo;
    private String cedula;
    private String correo;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;

    public ChoferDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdVinculo() {
        return idVinculo;
    }

    public void setIdVinculo(Long idVinculo) {
        this.idVinculo = idVinculo;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
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
