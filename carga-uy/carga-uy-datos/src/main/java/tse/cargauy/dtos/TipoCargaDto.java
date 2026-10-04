package tse.cargauy.dtos;

import java.io.Serializable;

public class TipoCargaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;

    public TipoCargaDto() {
    }

    public TipoCargaDto(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
