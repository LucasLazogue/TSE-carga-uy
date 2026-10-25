package tse.cargauy.nodobalanzas.negocio.pesaje;

import java.time.LocalDate;
import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;

@Local
public interface PesajeEJBLocal {
    List<PesajeDto> getPesajes(String matricula, LocalDate fecha);
    void addPesajes(List<PesajeDto> lecturas);
}
