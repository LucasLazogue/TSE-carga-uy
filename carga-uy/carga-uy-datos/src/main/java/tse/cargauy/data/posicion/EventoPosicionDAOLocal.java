package tse.cargauy.data.posicion;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.EventoPosicionDto;

@Local
public interface EventoPosicionDAOLocal {
    // false si ese nodo ya habia mandado un evento con el mismo id
    boolean registrar(EventoPosicionDto eventoDto);
    List<EventoPosicionDto> getRuta(String matricula);
}
