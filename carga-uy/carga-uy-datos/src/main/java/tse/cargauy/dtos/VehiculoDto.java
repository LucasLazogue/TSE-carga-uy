package tse.cargauy.dtos;

import java.io.Serializable;

public class VehiculoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String matricula;
    private String marca;
    private String modelo;
    private int pesoVehiculo;
    private int capacidadCarga;
    private Long idEmpresa;
    private String nombreEmpresa;

    public VehiculoDto() {
    }

    public VehiculoDto(String matricula, String marca, String modelo, int pesoVehiculo, int capacidadCarga, Long idEmpresa) {
        this.matricula = matricula;
        this.marca = marca;
        this.modelo = modelo;
        this.pesoVehiculo = pesoVehiculo;
        this.capacidadCarga = capacidadCarga;
        this.idEmpresa = idEmpresa;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public int getPesoVehiculo() {
        return pesoVehiculo;
    }

    public void setPesoVehiculo(int pesoVehiculo) {
        this.pesoVehiculo = pesoVehiculo;
    }

    public int getCapacidadCarga() {
        return capacidadCarga;
    }

    public void setCapacidadCarga(int capacidadCarga) {
        this.capacidadCarga = capacidadCarga;
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

    @Override
    public String toString() {
        return "Matrícula: " + matricula + ", Marca: " + marca + ", Modelo: " + modelo +
                ", Peso: " + pesoVehiculo + " kg, Capacidad de Carga: " + capacidadCarga + " kg, Empresa: " + nombreEmpresa;
    }
}
