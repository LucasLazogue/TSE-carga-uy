package tse.cargauy.dtos;

import java.io.Serializable;

public class ViajeMovilDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private ViajeDto viaje;
    private GuiaDto guia;

    public ViajeMovilDto() {
    }

    public ViajeMovilDto(ViajeDto viaje, GuiaDto guia) {
        this.viaje = viaje;
        this.guia = guia;
    }

    public ViajeDto getViaje() {
        return viaje;
    }

    public void setViaje(ViajeDto viaje) {
        this.viaje = viaje;
    }

    public GuiaDto getGuia() {
        return guia;
    }

    public void setGuia(GuiaDto guia) {
        this.guia = guia;
    }
}
