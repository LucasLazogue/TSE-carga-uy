package tse.cargauy.dtos;

import java.io.Serializable;

public class VolumenPorRubroDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String rubro;
    private long volumen;

    public VolumenPorRubroDto() {
    }

    public VolumenPorRubroDto(String rubro, long volumen) {
        this.rubro = rubro;
        this.volumen = volumen;
    }

    public String getRubro() {
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public long getVolumen() {
        return volumen;
    }

    public void setVolumen(long volumen) {
        this.volumen = volumen;
    }
}
