package tse.cargauy.negocio.fiscalizacion;

import java.time.LocalDate;
import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.NormaFiscalizacionDto;

@Local
public interface NormaFiscalizacionEJBLocal {

    List<NormaFiscalizacionDto> getVigentes();

    List<NormaFiscalizacionDto> getHistorial(String clave);

    void cambiarValor(String clave, int valor, LocalDate vigenteDesde);
}
