package tse.cargauy.negocio.movil;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import tse.cargauy.data.eventoviaje.EventoViajeDAOLocal;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.ResultadoEventoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.ResultadoEvento;
import tse.cargauy.entities.Rol;
import tse.cargauy.entities.TipoEventoViaje;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.exceptions.MensajesError;

// esta separado de ViajeChoferEJB para que REQUIRES_NEW tenga efecto: una llamada dentro de la misma clase
// no pasa por el contenedor y todos los eventos quedarian en una sola transaccion
@Stateless
@RolesAllowed(Rol.CHOFER)
public class AplicadorEventosEJB implements AplicadorEventosEJBLocal {

    // los viajes guardan la hora local sin zona; la zona es la del servidor (CARGAUY_ZONA_HORARIA)
    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);

    @EJB
    EventoViajeDAOLocal eventoDAO;

    @EJB
    ViajeDAOLocal viajeDAO;

    @EJB
    GuiaDAOLocal guiaDAO;

    // un evento con problemas se registra con su resultado y no deshace a los demas;
    // por eso no se tira CargaUYException, que haria rollback tambien del registro
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResultadoEventoDto aplicar(EventoViajeDto evento, Long idChofer) {
        if (eventoDAO.existe(evento.getIdEvento())) {
            return new ResultadoEventoDto(evento.getIdEvento(), ResultadoEvento.DUPLICADO, null);
        }
        String idViaje = String.valueOf(evento.getIdViaje());
        ViajeDto viaje = viajeDAO.getViajeById(evento.getIdViaje());
        if (viaje == null) {
            return registrar(evento, idChofer, ResultadoEvento.CONFLICTO, CodigoError.VIAJE_NO_ENCONTRADO, idViaje);
        }
        if (!idChofer.equals(viaje.getIdChofer())) {
            return registrar(evento, idChofer, ResultadoEvento.CONFLICTO, CodigoError.VIAJE_NO_ES_DEL_CHOFER, idViaje);
        }
        if (!fechaValida(evento.getTimestampGeneracion(), viaje)) {
            return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.EVENTO_FECHA_INVALIDA,
                    evento.getTimestampGeneracion().toString());
        }
        EstadoViaje nuevo = transicion(viaje.getEstado(), evento.getTipo());
        if (nuevo == null) {
            return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.VIAJE_TRANSICION_INVALIDA,
                    viaje.getEstado().name(), evento.getTipo().name());
        }
        if (nuevo != viaje.getEstado()) {
            viajeDAO.cambiarEstado(viaje.getId(), nuevo, LocalDateTime.ofInstant(evento.getTimestampGeneracion(), ZONA));
        }
        return registrar(evento, idChofer, ResultadoEvento.APLICADO, null);
    }

    // null si el evento no corresponde al estado del viaje
    static EstadoViaje transicion(EstadoViaje actual, TipoEventoViaje tipo) {
        return switch (tipo) {
            case INICIO -> actual == EstadoViaje.ASIGNADO ? EstadoViaje.EN_CURSO : null;
            case CARGA_PARCIAL, DESCARGA_PARCIAL, INCIDENTE -> actual == EstadoViaje.EN_CURSO ? EstadoViaje.EN_CURSO : null;
            case FIN -> actual == EstadoViaje.EN_CURSO ? EstadoViaje.FINALIZADO : null;
        };
    }

    private boolean fechaValida(Instant fecha, ViajeDto viaje) {
        Instant inicioGuia = guiaDAO.getGuiaById(viaje.getIdGuia()).getFecha().atStartOfDay(ZONA).toInstant();
        return !fecha.isBefore(inicioGuia) && !fecha.isAfter(Instant.now().plus(TOLERANCIA_FUTURO));
    }

    private ResultadoEventoDto registrar(EventoViajeDto evento, Long idChofer, ResultadoEvento resultado,
            CodigoError codigo, String... parametros) {
        String mensaje = codigo == null ? null : MensajesError.resolver(codigo, parametros);
        eventoDAO.registrar(evento, idChofer, resultado, mensaje);
        return new ResultadoEventoDto(evento.getIdEvento(), resultado, mensaje);
    }
}
