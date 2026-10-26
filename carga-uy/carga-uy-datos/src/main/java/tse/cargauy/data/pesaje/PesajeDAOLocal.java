package tse.cargauy.data.pesaje;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.PesajeDto;

@Local
public interface PesajeDAOLocal {
    boolean existe(String idOrigen);
    void registrar(PesajeDto pesaje);
    List<PesajeDto> getByVehiculo(Long idVehiculo, LocalDateTime desde, LocalDateTime hasta);
}
