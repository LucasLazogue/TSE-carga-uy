package tse.cargauy.dtos;

import java.io.Serializable;

public class VolumenPorRubroPublicoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String rubro;
    private Long volumen;

    public VolumenPorRubroPublicoDto() {
    }

    public VolumenPorRubroPublicoDto(String rubro, Long volumen) {
        this.rubro = rubro;
        this.volumen = volumen;
    }

    public String getRubro() {
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public Long getVolumen() {
        return volumen;
    }

    public void setVolumen(Long volumen) {
        this.volumen = volumen;
    }
}
