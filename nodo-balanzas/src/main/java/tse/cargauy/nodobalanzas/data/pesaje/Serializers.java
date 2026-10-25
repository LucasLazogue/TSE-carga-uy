package tse.cargauy.nodobalanzas.data.pesaje;

import tse.cargauy.nodobalanzas.dtos.BalanzaDto;
import tse.cargauy.nodobalanzas.dtos.PeajeDto;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;
import tse.cargauy.nodobalanzas.entities.Balanza;
import tse.cargauy.nodobalanzas.entities.Peaje;
import tse.cargauy.nodobalanzas.entities.Pesaje;

public class Serializers {
    public static PesajeDto toDto(Pesaje pesaje) {
        Balanza balanza = pesaje.getBalanza();
        Peaje peaje = balanza.getPeaje();
        PesajeDto dto = new PesajeDto();
        dto.setId(pesaje.getId());
        dto.setMatricula(pesaje.getMatricula());
        dto.setRutEmpresa(pesaje.getRutEmpresa());
        dto.setFechaHora(pesaje.getFechaHora());
        dto.setPesoRegistrado(pesaje.getPesoRegistrado());
        dto.setBalanza(new BalanzaDto(balanza.getCodigo(), balanza.getSentido(), null));
        dto.setPeaje(new PeajeDto(peaje.getCodigo(), peaje.getNombre(), peaje.getRuta(), peaje.getKm(),
                peaje.getLatitud(), peaje.getLongitud()));
        return dto;
    }
}
