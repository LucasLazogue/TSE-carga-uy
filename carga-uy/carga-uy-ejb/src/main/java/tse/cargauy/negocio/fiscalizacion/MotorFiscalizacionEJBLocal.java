package tse.cargauy.negocio.fiscalizacion;

import jakarta.ejb.Local;

@Local
public interface MotorFiscalizacionEJBLocal {

    // una posicion ya guardada en el almacen de posiciones
    void evaluarPosicion(Long idNodo, String idEvento);

    // un viaje que el chofer termino desde el movil
    void evaluarViaje(Long idViaje);
}
