package tse.cargauy.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Notificacion {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    private CasoFiscalizacion caso;

    @ManyToOne(optional = false)
    private Empresa empresa;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(columnDefinition = "text", nullable = false)
    private String mensaje;

    public Notificacion() {
    }

    public Notificacion(CasoFiscalizacion caso, LocalDateTime fecha, String mensaje) {
        this.caso = caso;
        this.empresa = caso.getEmpresa();
        this.fecha = fecha;
        this.mensaje = mensaje;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CasoFiscalizacion getCaso() {
        return caso;
    }

    public void setCaso(CasoFiscalizacion caso) {
        this.caso = caso;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

}
