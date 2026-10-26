package tse.cargauy.negocio.tracking;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import tse.cargauy.data.nodo.NodoPerifericoDAOLocal;
import tse.cargauy.data.posicion.EventoPosicionDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.TipoNodo;

// CU-10
@Stateless
public class IngestaTrackingEJB implements IngestaTrackingEJBLocal {

    // los viajes guardan la hora local sin zona; la zona es la del servidor (CARGAUY_ZONA_HORARIA)
    private static final ZoneId ZONA = ZoneId.systemDefault();

    @EJB
    NodoPerifericoDAOLocal nodoDAO;

    @EJB
    VehiculoDAOLocal vehiculoDAO;

    @EJB
    EventoPosicionDAOLocal eventoDAO;

    @EJB
    ViajeDAOLocal viajeDAO;

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
        // una matricula desconocida se guarda igual, con la matricula y sin vehiculo: si despues se registra el
        // vehiculo, sus viajes encuentran estas posiciones por la matricula y no se pierde informacion
        VehiculoDto vehiculo = vehiculoDAO.getVehiculoByMatricula(eventoDto.getMatricula());
        eventoDto.setIdVehiculo(vehiculo == null ? null : vehiculo.getId());
        // el viaje en curso cuando se genero la posicion, no cuando llego (paso 5 de la ficha). Si el inicio del viaje
        // todavia no llego desde el movil, queda sin viaje y se asocia cuando llegue (ReasociadorPosiciones)
        ViajeDto viaje = vehiculo == null ? null
                : viajeDAO.getEnCursoDelVehiculo(vehiculo.getId(), LocalDateTime.ofInstant(eventoDto.getTimestampGeneracion(), ZONA));
        eventoDto.setIdViaje(viaje == null ? null : viaje.getId());
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

    // los pasos que van despues de guardar la posicion, cuando se confirma la transaccion: la marca de procesada
    // (MarcadorPosiciones) y la evaluacion de las reglas de fiscalizacion (DisparadorFiscalizacion)
    private void terminar(EventoPosicionDto posicion) {
        procesadas.fire(new PosicionProcesada(posicion.getIdNodo(), posicion.getIdEvento()));
    }
}
