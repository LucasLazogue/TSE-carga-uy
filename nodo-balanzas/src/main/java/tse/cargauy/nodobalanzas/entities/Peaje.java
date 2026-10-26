package tse.cargauy.nodobalanzas.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;

@Entity
@NamedQueries({
    @NamedQuery(name = Peaje.TODOS,
            query = "SELECT p FROM Peaje p ORDER BY p.codigo"),
    @NamedQuery(name = Peaje.POR_CODIGO,
            query = "SELECT p FROM Peaje p WHERE p.codigo = :codigo")
})
public class Peaje {

    public static final String TODOS = "Peaje.todos";
    public static final String POR_CODIGO = "Peaje.porCodigo";

    @Id @GeneratedValue
    private Long id;
    @Column(unique = true, nullable = false)
    private String codigo;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false)
    private String ruta;
    private int km;
    private double latitud;
    private double longitud;
    @OneToMany(mappedBy = "peaje")
    private List<Balanza> balanzas = new ArrayList<>();

    public Peaje() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public int getKm() {
        return km;
    }

    public void setKm(int km) {
        this.km = km;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public List<Balanza> getBalanzas() {
        return balanzas;
    }

    public void setBalanzas(List<Balanza> balanzas) {
        this.balanzas = balanzas;
    }
}
