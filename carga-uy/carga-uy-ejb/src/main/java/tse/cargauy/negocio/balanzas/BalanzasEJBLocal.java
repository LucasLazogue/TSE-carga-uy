package tse.cargauy.negocio.balanzas;

import jakarta.ejb.Local;
import tse.cargauy.dtos.PesajesViajeDto;
import tse.cargauy.dtos.ViajeDto;

@Local
public interface BalanzasEJBLocal {

    // los pesajes del vehiculo en el horario del viaje, de todos los nodos de balanzas habilitados
    PesajesViajeDto getPesajesDelViaje(ViajeDto viaje);
}
