package tse.cargauy.nodobalanzas.data.pesaje;

import tse.cargauy.nodobalanzas.dtos.BalanzaDto;
import tse.cargauy.nodobalanzas.dtos.PeajeDto;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;
import tse.cargauy.nodobalanzas.entities.Balanza;
import tse.cargauy.nodobalanzas.entities.Peaje;
import tse.cargauy.nodobalanzas.entities.Pesaje;

public class Serializers {
    public static PesajeDto toDto(Pesaje pesaje) {
        PesajeDto dto = new PesajeDto();
        dto.setId(pesaje.getId());
        dto.setMatricula(pesaje.getMatricula());
        dto.setRutEmpresa(pesaje.getRutEmpresa());
        dto.setFechaHora(pesaje.getFechaHora());
        dto.setPesoRegistrado(pesaje.getPesoRegistrado());
        dto.setBalanza(toDto(pesaje.getBalanza()));
        dto.setPeaje(toDto(pesaje.getBalanza().getPeaje()));
        return dto;
    }

    public static BalanzaDto toDto(Balanza balanza) {
        BalanzaDto dto = new BalanzaDto();
        dto.setCodigo(balanza.getCodigo());
        dto.setSentido(balanza.getSentido());
        return dto;
    }

    public static PeajeDto toDto(Peaje peaje) {
        PeajeDto dto = new PeajeDto();
        dto.setCodigo(peaje.getCodigo());
        dto.setNombre(peaje.getNombre());
        dto.setRuta(peaje.getRuta());
        dto.setKm(peaje.getKm());
        dto.setLatitud(peaje.getLatitud());
        dto.setLongitud(peaje.getLongitud());
        return dto;
    }
}
