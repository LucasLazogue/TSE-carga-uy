package tse.cargauy.nodobalanzas.data.peaje;

import java.util.ArrayList;
import java.util.List;

import tse.cargauy.nodobalanzas.dtos.BalanzaDto;
import tse.cargauy.nodobalanzas.dtos.PeajeDto;
import tse.cargauy.nodobalanzas.entities.Balanza;
import tse.cargauy.nodobalanzas.entities.Peaje;

public class Serializers {
    public static PeajeDto toDto(Peaje peaje) {
        return new PeajeDto(peaje.getCodigo(), peaje.getNombre(), peaje.getRuta(), peaje.getKm(),
                peaje.getLatitud(), peaje.getLongitud());
    }

    public static PeajeDto toDtoConBalanzas(Peaje peaje) {
        PeajeDto dto = toDto(peaje);
        List<BalanzaDto> balanzas = new ArrayList<>();
        for (Balanza b : peaje.getBalanzas()) {
            balanzas.add(toDto(b));
        }
        dto.setBalanzas(balanzas);
        return dto;
    }

    public static BalanzaDto toDto(Balanza balanza) {
        return new BalanzaDto(balanza.getCodigo(), balanza.getSentido(), null);
    }

    public static Peaje toEntity(PeajeDto dto) {
        Peaje peaje = new Peaje();
        peaje.setCodigo(dto.getCodigo());
        peaje.setNombre(dto.getNombre());
        peaje.setRuta(dto.getRuta());
        peaje.setKm(dto.getKm());
        peaje.setLatitud(dto.getLatitud());
        peaje.setLongitud(dto.getLongitud());
        return peaje;
    }
}
