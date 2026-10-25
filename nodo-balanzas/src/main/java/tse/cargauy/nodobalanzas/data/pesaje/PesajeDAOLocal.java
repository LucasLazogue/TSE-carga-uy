package tse.cargauy.nodobalanzas.data.pesaje;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;

@Local
public interface PesajeDAOLocal {
    List<PesajeDto> getPesajesByMatricula(String matricula, LocalDateTime desde, LocalDateTime hasta);
    List<PesajeDto> getPesajesByRut(String rut, LocalDateTime desde, LocalDateTime hasta);
}
