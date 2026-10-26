package tse.cargauy.dtos;

import java.io.Serializable;

public class RechazoPermisoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String motivo;

    public RechazoPermisoDto() {
    }

    public RechazoPermisoDto(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
