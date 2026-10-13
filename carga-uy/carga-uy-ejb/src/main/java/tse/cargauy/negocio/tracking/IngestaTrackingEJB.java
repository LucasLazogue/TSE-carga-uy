package tse.cargauy.negocio.tracking;

import java.time.Instant;
import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import tse.cargauy.data.nodo.NodoPerifericoDAOLocal;
import tse.cargauy.data.posicion.EventoPosicionDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.TipoNodo;

// CU-10. Todavia no asocia el evento al viaje en curso (paso 5 de la ficha) porque Viaje no existe en el modelo
@Stateless
public class IngestaTrackingEJB implements IngestaTrackingEJBLocal {

    @EJB
    NodoPerifericoDAOLocal nodoDAO;

    @EJB
    VehiculoDAOLocal vehiculoDAO;

    @EJB
    EventoPosicionDAOLocal eventoDAO;

    @Inject
    Event<PosicionProcesada> procesadas;

    public ResultadoIngesta procesar(EventoPosicionDto eventoDto) {
        NodoPerifericoDto nodo = nodoDAO.getByIdentificador(eventoDto.getNodo());
        if (nodo == null) {
            return ResultadoIngesta.NODO_DESCONOCIDO;
        }
        // precondicion del CU-10: solo se acepta lo que manda un nodo de tracking registrado y habilitado
        if (nodo.getTipo() != TipoNodo.TRACKING || nodo.getEstado() != EstadoNodo.HABILITADO) {
            return ResultadoIngesta.NODO_NO_HABILITADO;
        }
        eventoDto.setIdNodo(nodo.getId());
        // una matricula desconocida se guarda igual, sin vehiculo: que hacer con ella todavia no esta decidido
        VehiculoDto vehiculo = vehiculoDAO.getVehiculoByMatricula(eventoDto.getMatricula());
        eventoDto.setIdVehiculo(vehiculo == null ? null : vehiculo.getId());
        eventoDto.setTimestampRecepcion(Instant.now());
        if (eventoDAO.registrar(eventoDto)) {
            terminar(eventoDto);
            return ResultadoIngesta.REGISTRADO;
        }
        // ya estaba guardada (DA07). Si quedo sin procesar, un intento anterior fallo despues de guardarla, o la esta
        // procesando otro consumidor: se repiten los pasos siguientes sobre la guardada, que se pueden repetir
        EventoPosicionDto guardada = eventoDAO.get(eventoDto.getIdNodo(), eventoDto.getIdEvento());
        if (guardada == null || guardada.isProcesada()) {
            return ResultadoIngesta.DUPLICADO;
        }
        terminar(guardada);
        return ResultadoIngesta.COMPLETADO;
    }

    // los pasos que van despues de guardar la posicion. Por ahora solo la marca de procesada
    private void terminar(EventoPosicionDto posicion) {
        procesadas.fire(new PosicionProcesada(posicion.getIdNodo(), posicion.getIdEvento()));
    }

    public List<EventoPosicionDto> getRuta(String matricula) {
        return eventoDAO.getRuta(matricula);
    }
}
