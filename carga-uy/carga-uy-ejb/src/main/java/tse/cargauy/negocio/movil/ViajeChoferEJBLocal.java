package tse.cargauy.negocio.movil;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.SyncRespuestaDto;
import tse.cargauy.dtos.ViajeMovilDto;

@Local
public interface ViajeChoferEJBLocal {

    ViajeMovilDto getViajeActual();

    SyncRespuestaDto sincronizar(List<EventoViajeDto> eventos);
}
