package tse.cargauy.data.posicion;

import java.time.Instant;
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
    // asocia al viaje (o a ninguno, con null) las posiciones de la matricula generadas en [desde, hasta);
    // hasta null = sin limite. Les pone tambien el vehiculo, por si se registro despues de recibirlas
    void asociarAViaje(String matricula, Long idVehiculo, Instant desde, Instant hasta, Long idViaje);
    List<EventoPosicionDto> getRuta(String matricula);
    List<EventoPosicionDto> getRutaDelViaje(Long idViaje);
}
