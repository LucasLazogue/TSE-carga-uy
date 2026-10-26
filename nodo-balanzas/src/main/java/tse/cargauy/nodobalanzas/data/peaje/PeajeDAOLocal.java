package tse.cargauy.nodobalanzas.data.peaje;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.nodobalanzas.dtos.BalanzaDto;
import tse.cargauy.nodobalanzas.dtos.PeajeDto;

@Local
public interface PeajeDAOLocal {
    List<PeajeDto> getPeajes();
    PeajeDto getPeajeByCodigo(String codigo);
    void addPeaje(PeajeDto peajeDto);
    BalanzaDto getBalanzaByCodigo(String codigo);
    String getCodigoBalanzaByClave(String claveHash);
    void addBalanza(String codigoPeaje, BalanzaDto balanzaDto, String claveHash);
}
