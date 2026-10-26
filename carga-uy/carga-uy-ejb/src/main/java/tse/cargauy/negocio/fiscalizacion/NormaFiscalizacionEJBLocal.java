package tse.cargauy.negocio.fiscalizacion;

import java.time.LocalDate;
import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.NormaFiscalizacionDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;

@Local
public interface NormaFiscalizacionEJBLocal {

    List<NormaFiscalizacionDto> getVigentes();

    PaginaDto<NormaFiscalizacionDto> getHistorial(String clave, Paginacion paginacion);

    void cambiarValor(String clave, int valor, LocalDate vigenteDesde);
}
