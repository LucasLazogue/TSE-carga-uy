package tse.cargauy.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    // los default son para las filas que ya existian cuando se agregaron las columnas
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'EMPRESA' not null")
    private DestinatarioNotificacion destinatario;

    @Column(nullable = false, columnDefinition = "boolean default false not null")
    private boolean leida;

    public Notificacion() {
    }

    public Notificacion(CasoFiscalizacion caso, DestinatarioNotificacion destinatario, LocalDateTime fecha, String mensaje) {
        this.caso = caso;
        this.empresa = caso.getEmpresa();
        this.destinatario = destinatario;
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

    public DestinatarioNotificacion getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(DestinatarioNotificacion destinatario) {
        this.destinatario = destinatario;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

}
