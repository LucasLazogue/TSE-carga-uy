package tse.cargauy.negocio.movil;

import jakarta.ejb.Local;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.ResultadoEventoDto;

@Local
public interface AplicadorEventosEJBLocal {

    ResultadoEventoDto aplicar(EventoViajeDto evento, Long idChofer);
}
