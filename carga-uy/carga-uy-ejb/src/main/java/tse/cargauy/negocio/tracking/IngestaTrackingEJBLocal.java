package tse.cargauy.negocio.tracking;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.EventoPosicionDto;

@Local
public interface IngestaTrackingEJBLocal {
    ResultadoIngesta procesar(EventoPosicionDto eventoDto);
    List<EventoPosicionDto> getRuta(String matricula);
}
