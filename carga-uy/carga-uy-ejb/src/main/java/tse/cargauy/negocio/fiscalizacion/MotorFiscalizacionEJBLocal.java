package tse.cargauy.negocio.fiscalizacion;

import io.opentelemetry.context.Context;
import jakarta.ejb.Local;

@Local
public interface MotorFiscalizacionEJBLocal {

    // una posicion ya guardada en el almacen de posiciones
    // traza: la de la ingesta de la posicion, de la que cuelga la evaluacion
    void evaluarPosicion(Long idNodo, String idEvento, Context traza);

    // un viaje que el chofer termino desde el movil
    // traza: la del pedido que registro el fin del viaje
    void evaluarViaje(Long idViaje, Context traza);
}
