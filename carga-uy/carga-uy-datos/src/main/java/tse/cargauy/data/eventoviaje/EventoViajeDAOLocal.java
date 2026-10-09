package tse.cargauy.data.eventoviaje;

import jakarta.ejb.Local;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.entities.ResultadoEvento;

@Local
public interface EventoViajeDAOLocal {
    boolean existe(String idEvento);
    void registrar(EventoViajeDto evento, Long idChofer, ResultadoEvento resultado, String detalle);
}