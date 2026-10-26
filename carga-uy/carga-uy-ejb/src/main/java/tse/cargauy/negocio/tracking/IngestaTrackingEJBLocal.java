package tse.cargauy.negocio.tracking;

import jakarta.ejb.Local;
import tse.cargauy.dtos.EventoPosicionDto;

@Local
public interface IngestaTrackingEJBLocal {
    ResultadoIngesta procesar(EventoPosicionDto eventoDto);
}
