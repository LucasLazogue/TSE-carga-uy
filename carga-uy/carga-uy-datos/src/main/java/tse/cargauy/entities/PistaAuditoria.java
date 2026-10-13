package tse.cargauy.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class PistaAuditoria {

    public static final String SISTEMA = "SISTEMA";

    @Id @GeneratedValue
    private Long id;

    // sin FK a proposito: la pista no depende de que el caso exista
    @Column(nullable = false)
    private Long idCaso;

    @Column(nullable = false)
    private String usuario;

    @Column(nullable = false)
    private String rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccionAuditoria accion;

    @Enumerated(EnumType.STRING)
    private EstadoCaso estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCaso estadoNuevo;

    @Column(columnDefinition = "text")
    private String fundamento;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public PistaAuditoria() {
    }

    public PistaAuditoria(Long idCaso, String usuario, String rol, AccionAuditoria accion, EstadoCaso estadoAnterior,
            EstadoCaso estadoNuevo, String fundamento, LocalDateTime fecha) {
        this.idCaso = idCaso;
        this.usuario = usuario;
        this.rol = rol;
        this.accion = accion;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.fundamento = fundamento;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public Long getIdCaso() {
        return idCaso;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getRol() {
        return rol;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public EstadoCaso getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoCaso getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getFundamento() {
        return fundamento;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

}
