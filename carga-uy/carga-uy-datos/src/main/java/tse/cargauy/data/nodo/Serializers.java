package tse.cargauy.data.nodo;

import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.entities.NodoPeriferico;

public class Serializers {
    public static NodoPerifericoDto toDto(NodoPeriferico nodo) {
        NodoPerifericoDto dto = new NodoPerifericoDto();
        dto.setId(nodo.getId());
        dto.setIdentificador(nodo.getIdentificador());
        dto.setTipo(nodo.getTipo());
        dto.setPuntoAcceso(nodo.getPuntoAcceso());
        dto.setEstado(nodo.getEstado());
        return dto;
    }

    public static NodoPeriferico toEntity(NodoPerifericoDto dto) {
        NodoPeriferico nodo = new NodoPeriferico();
        nodo.setIdentificador(dto.getIdentificador());
        nodo.setTipo(dto.getTipo());
        nodo.setPuntoAcceso(dto.getPuntoAcceso());
        nodo.setEstado(dto.getEstado());
        return nodo;
    }
}
