package tse.cargauy.data.eventoviaje;

import jakarta.ejb.Local;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.entities.ResultadoEvento;

@Local
public interface EventoViajeDAOLocal {
    boolean existe(String idEvento);
    void registrar(EventoViajeDto evento, Long idChofer, ResultadoEvento resultado, String detalle);
    PaginaDto<EventoViajeDto> getByViaje(Long idViaje, Paginacion paginacion);
}