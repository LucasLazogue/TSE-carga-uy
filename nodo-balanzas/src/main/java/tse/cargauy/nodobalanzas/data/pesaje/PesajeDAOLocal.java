package tse.cargauy.nodobalanzas.data.pesaje;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;

@Local
public interface PesajeDAOLocal {
    List<PesajeDto> getPesajesByMatricula(String matricula, LocalDateTime desde, LocalDateTime hasta);
    boolean existePesaje(String codigoBalanza, String idRegistro);
    void addPesaje(String codigoBalanza, PesajeDto pesajeDto, LocalDateTime recibidoEn);
}
