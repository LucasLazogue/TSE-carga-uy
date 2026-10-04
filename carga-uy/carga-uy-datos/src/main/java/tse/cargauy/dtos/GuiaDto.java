package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class GuiaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nroGuia;
    private LocalDate fecha;
    private String origen;
    private String destino;
    private int volumen;
    private Long idEmpresa;
    private String nombreEmpresa;
    private Long idRegistradaPor;
    private Long idRubro;
    private String nombreRubro;
    private Long idTipoCarga;
    private String nombreTipoCarga;
    private Long idViaje;

    public GuiaDto() {
    }

    public GuiaDto(String nroGuia, LocalDate fecha, String origen, String destino, int volumen, Long idEmpresa, Long idRegistradaPor, Long idRubro, Long idTipoCarga) {
        this.nroGuia = nroGuia;
        this.fecha = fecha;
        this.origen = origen;
        this.destino = destino;
        this.volumen = volumen;
        this.idEmpresa = idEmpresa;
        this.idRegistradaPor = idRegistradaPor;
        this.idRubro = idRubro;
        this.idTipoCarga = idTipoCarga;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNroGuia() {
        return nroGuia;
    }

    public void setNroGuia(String nroGuia) {
        this.nroGuia = nroGuia;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public int getVolumen() {
        return volumen;
    }

    public void setVolumen(int volumen) {
        this.volumen = volumen;
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

    public Long getIdRegistradaPor() {
        return idRegistradaPor;
    }

    public void setIdRegistradaPor(Long idRegistradaPor) {
        this.idRegistradaPor = idRegistradaPor;
    }

    public Long getIdRubro() {
        return idRubro;
    }

    public void setIdRubro(Long idRubro) {
        this.idRubro = idRubro;
    }

    public String getNombreRubro() {
        return nombreRubro;
    }

    public void setNombreRubro(String nombreRubro) {
        this.nombreRubro = nombreRubro;
    }

    public Long getIdTipoCarga() {
        return idTipoCarga;
    }

    public void setIdTipoCarga(Long idTipoCarga) {
        this.idTipoCarga = idTipoCarga;
    }

    public String getNombreTipoCarga() {
        return nombreTipoCarga;
    }

    public void setNombreTipoCarga(String nombreTipoCarga) {
        this.nombreTipoCarga = nombreTipoCarga;
    }

    public Long getIdViaje() {
        return idViaje;
    }

    public void setIdViaje(Long idViaje) {
        this.idViaje = idViaje;
    }

    @Override
    public String toString() {
        return "Nro. Guia: " + nroGuia + ", Fecha: " + fecha + ", Origen: " + origen + ", Destino: " + destino +
                ", Volumen: " + volumen + " kg, Rubro: " + nombreRubro + ", Tipo de Carga: " + nombreTipoCarga + ", Empresa: " + nombreEmpresa;
    }
}
