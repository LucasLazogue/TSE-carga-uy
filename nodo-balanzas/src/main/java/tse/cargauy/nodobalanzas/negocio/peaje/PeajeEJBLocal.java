package tse.cargauy.nodobalanzas.negocio.peaje;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.nodobalanzas.dtos.BalanzaDto;
import tse.cargauy.nodobalanzas.dtos.PeajeDto;

@Local
public interface PeajeEJBLocal {
    List<PeajeDto> getPeajes();
    void addPeaje(PeajeDto peajeDto);
    BalanzaDto addBalanza(String codigoPeaje, BalanzaDto balanzaDto);
}
