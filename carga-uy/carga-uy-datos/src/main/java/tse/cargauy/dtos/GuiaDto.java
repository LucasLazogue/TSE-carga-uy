package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.LocalDate;

public class GuiaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nroGuia;
    private LocalDate fecha;
    private Double origenLat;
    private Double origenLon;
    private Double destinoLat;
    private Double destinoLon;
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

    public GuiaDto(LocalDate fecha, Double origenLat, Double origenLon, Double destinoLat, Double destinoLon, int volumen, Long idEmpresa, Long idRegistradaPor, Long idRubro, Long idTipoCarga) {
        this.fecha = fecha;
        this.origenLat = origenLat;
        this.origenLon = origenLon;
        this.destinoLat = destinoLat;
        this.destinoLon = destinoLon;
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

    public Double getOrigenLat() {
        return origenLat;
    }

    public void setOrigenLat(Double origenLat) {
        this.origenLat = origenLat;
    }

    public Double getOrigenLon() {
        return origenLon;
    }

    public void setOrigenLon(Double origenLon) {
        this.origenLon = origenLon;
    }

    public Double getDestinoLat() {
        return destinoLat;
    }

    public void setDestinoLat(Double destinoLat) {
        this.destinoLat = destinoLat;
    }

    public Double getDestinoLon() {
        return destinoLon;
    }

    public void setDestinoLon(Double destinoLon) {
        this.destinoLon = destinoLon;
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
        return "Nro. Guia: " + nroGuia + ", Fecha: " + fecha + ", Origen: (" + origenLat + ", " + origenLon + ")" +
                ", Destino: (" + destinoLat + ", " + destinoLon + "), Volumen: " + volumen + " kg, Rubro: " + nombreRubro +
                ", Tipo de Carga: " + nombreTipoCarga + ", Empresa: " + nombreEmpresa;
    }
}
