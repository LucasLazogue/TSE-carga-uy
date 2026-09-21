package tse.cargauy.entities;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Vehiculo {

    @Id @GeneratedValue
    private Long id;
    @Column(unique = true, nullable = false)
    private String matricula;
    private String marca;
    private String modelo;
    private int pesoVehiculo;
    private int capacidadCarga;
    @ManyToOne(optional = false)
    private Empresa empresa;
    @OneToMany(mappedBy = "vehiculo", cascade = CascadeType.REMOVE)
    private List<Permiso> permisos;

    public Vehiculo() {
    }

    public Vehiculo(String matricula, String marca, String modelo, int pesoVehiculo, int capacidadCarga, Empresa empresa) {
        this.matricula = matricula;
        this.marca = marca;
        this.modelo = modelo;
        this.pesoVehiculo = pesoVehiculo;
        this.capacidadCarga = capacidadCarga;
        this.empresa = empresa;
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

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public List<Permiso> getPermisos() {
        return permisos;
    }

    public void setPermisos(List<Permiso> permisos) {
        this.permisos = permisos;
    }

}
