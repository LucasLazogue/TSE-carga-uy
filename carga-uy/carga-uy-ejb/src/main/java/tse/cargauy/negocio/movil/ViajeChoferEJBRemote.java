package tse.cargauy.negocio.movil;

import java.util.List;

import jakarta.ejb.Remote;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.SyncRespuestaDto;
import tse.cargauy.dtos.ViajeMovilDto;

@Remote
public interface ViajeChoferEJBRemote {

    List<ViajeMovilDto> getViajes();

    SyncRespuestaDto sincronizar(List<EventoViajeDto> eventos);
}
