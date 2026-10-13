package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoInfraccion;

public class CasoFiscalizacionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Long id;
    private TipoInfraccion tipo;
    private EstadoCaso estado;
    private String claveDeduplicacion;
    private LocalDateTime fechaHecho;
    private LocalDateTime fechaDeteccion;
    private LocalDateTime fechaNotificacion;
    private LocalDateTime venceDescargo;
    private Long idVehiculo;
    private String matricula;
    private Long idEmpresa;
    private String nombreEmpresa;
    private Long idViaje;
    private List<EvidenciaDto> evidencias = new ArrayList<>();
    private DescargoDto descargo;
    private ResolucionDto resolucion;

    public CasoFiscalizacionDto() {
    }

    public CasoFiscalizacionDto(TipoInfraccion tipo, String claveDeduplicacion, LocalDateTime fechaHecho, Long idVehiculo, Long idViaje) {
        this.tipo = tipo;
        this.claveDeduplicacion = claveDeduplicacion;
        this.fechaHecho = fechaHecho;
        this.idVehiculo = idVehiculo;
        this.idViaje = idViaje;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoInfraccion getTipo() {
        return tipo;
    }

    public void setTipo(TipoInfraccion tipo) {
        this.tipo = tipo;
    }

    public EstadoCaso getEstado() {
        return estado;
    }

    public void setEstado(EstadoCaso estado) {
        this.estado = estado;
    }

    public String getClaveDeduplicacion() {
        return claveDeduplicacion;
    }

    public void setClaveDeduplicacion(String claveDeduplicacion) {
        this.claveDeduplicacion = claveDeduplicacion;
    }

    public LocalDateTime getFechaHecho() {
        return fechaHecho;
    }

    public void setFechaHecho(LocalDateTime fechaHecho) {
        this.fechaHecho = fechaHecho;
    }

    public LocalDateTime getFechaDeteccion() {
        return fechaDeteccion;
    }

    public void setFechaDeteccion(LocalDateTime fechaDeteccion) {
        this.fechaDeteccion = fechaDeteccion;
    }

    public LocalDateTime getFechaNotificacion() {
        return fechaNotificacion;
    }

    public void setFechaNotificacion(LocalDateTime fechaNotificacion) {
        this.fechaNotificacion = fechaNotificacion;
    }

    public LocalDateTime getVenceDescargo() {
        return venceDescargo;
    }

    public void setVenceDescargo(LocalDateTime venceDescargo) {
        this.venceDescargo = venceDescargo;
    }

    public Long getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(Long idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getNombreEmpresa() {
        return nombreEmpresa;
    }

    public void setNombreEmpresa(String nombreEmpresa) {
        this.nombreEmpresa = nombreEmpresa;
    }

    public Long getIdViaje() {
        return idViaje;
    }

    public void setIdViaje(Long idViaje) {
        this.idViaje = idViaje;
    }

    public List<EvidenciaDto> getEvidencias() {
        return evidencias;
    }

    public void setEvidencias(List<EvidenciaDto> evidencias) {
        this.evidencias = evidencias;
    }

    public DescargoDto getDescargo() {
        return descargo;
    }

    public void setDescargo(DescargoDto descargo) {
        this.descargo = descargo;
    }

    public ResolucionDto getResolucion() {
        return resolucion;
    }

    public void setResolucion(ResolucionDto resolucion) {
        this.resolucion = resolucion;
    }
}
