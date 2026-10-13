package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DescargoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String texto;
    private LocalDateTime fecha;
    private String cedulaPresentadoPor;

    public DescargoDto() {
    }

    public DescargoDto(String texto) {
        this.texto = texto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getCedulaPresentadoPor() {
        return cedulaPresentadoPor;
    }

    public void setCedulaPresentadoPor(String cedulaPresentadoPor) {
        this.cedulaPresentadoPor = cedulaPresentadoPor;
    }
}
