package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

import tse.cargauy.entities.AccionAuditoria;
import tse.cargauy.entities.EstadoCaso;

public class PistaAuditoriaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long idCaso;
    private String usuario;
    private String rol;
    private AccionAuditoria accion;
    private EstadoCaso estadoAnterior;
    private EstadoCaso estadoNuevo;
    private String fundamento;
    private LocalDateTime fecha;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdCaso() {
        return idCaso;
    }

    public void setIdCaso(Long idCaso) {
        this.idCaso = idCaso;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public void setAccion(AccionAuditoria accion) {
        this.accion = accion;
    }

    public EstadoCaso getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoCaso estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoCaso getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoCaso estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getFundamento() {
        return fundamento;
    }

    public void setFundamento(String fundamento) {
        this.fundamento = fundamento;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
