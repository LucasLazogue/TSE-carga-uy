package tse.cargauy.data.posicion;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.EventoPosicionDto;

@Local
public interface EventoPosicionDAOLocal {
    // la guarda sin procesar. false si ese nodo ya habia mandado un evento con el mismo id
    boolean registrar(EventoPosicionDto eventoDto);
    // la que ya esta guardada con ese nodo e id de evento, o null
    EventoPosicionDto get(Long idNodo, String idEvento);
    void marcarProcesada(Long idNodo, String idEvento);
    List<EventoPosicionDto> getRuta(String matricula);
}
