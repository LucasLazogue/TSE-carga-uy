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
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import tse.cargauy.data.eventoviaje.EventoViajeDAOLocal;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.ResultadoEventoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.EventoViaje;
import tse.cargauy.entities.ResultadoEvento;
import tse.cargauy.entities.Rol;
import tse.cargauy.entities.TipoEventoViaje;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.exceptions.MensajesError;
import tse.cargauy.negocio.viaje.ViajeIniciadoOFinalizado;

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

    @Inject
    Event<ViajeIniciadoOFinalizado> inicioOFin;

    // un evento con problemas se registra con su resultado y no deshace a los demas;
    // por eso no se tira CargaUYException, que haria rollback tambien del registro
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResultadoEventoDto aplicar(EventoViajeDto evento, Long idChofer) {
        if (eventoDAO.existe(evento.getIdEvento())) {
            return new ResultadoEventoDto(evento.getIdEvento(), ResultadoEvento.DUPLICADO, null);
        }
        normalizar(evento);
        String idViaje = String.valueOf(evento.getIdViaje());
        // otro envio sobre el mismo viaje (el otro celular, o la web) espera a que termine esta transaccion
        ViajeDto viaje = viajeDAO.getViajeParaActualizar(evento.getIdViaje());
        if (viaje == null) {
            return registrar(evento, idChofer, ResultadoEvento.CONFLICTO, CodigoError.VIAJE_NO_ENCONTRADO, idViaje);
        }
        if (!idChofer.equals(viaje.getIdChofer())) {
            return registrar(evento, idChofer, ResultadoEvento.CONFLICTO, CodigoError.VIAJE_NO_ES_DEL_CHOFER, idViaje);
        }
        GuiaDto guia = guiaDAO.getGuiaById(viaje.getIdGuia());
        if (!fechaValida(evento.getTimestampGeneracion(), guia)) {
            return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.EVENTO_FECHA_INVALIDA,
                    evento.getTimestampGeneracion().toString());
        }
        ResultadoEventoDto datosInvalidos = validarDatos(evento, idChofer, guia);
        if (datosInvalidos != null) {
            return datosInvalidos;
        }
        LocalDateTime hora = LocalDateTime.ofInstant(evento.getTimestampGeneracion(), ZONA);
        return switch (evento.getTipo()) {
            case INICIO -> iniciar(evento, idChofer, viaje, hora);
            case FIN -> finalizar(evento, idChofer, viaje, hora);
            case CARGA_PARCIAL, DESCARGA_PARCIAL, INCIDENTE -> registrarDurante(evento, idChofer, viaje, hora);
        };
    }

    // un inicio que llega tarde con una hora anterior a la registrada la corrige: vale el mas temprano.
    // uno posterior no corresponde, porque el viaje ya habia empezado
    private ResultadoEventoDto iniciar(EventoViajeDto evento, Long idChofer, ViajeDto viaje, LocalDateTime hora) {
        boolean asignado = viaje.getEstado() == EstadoViaje.ASIGNADO;
        if (!asignado && !hora.isBefore(viaje.getFechaInicio())) {
            return transicionInvalida(evento, idChofer, viaje);
        }
        // el tramo que el viaje pasa a ocupar: desde esta hora hasta el inicio que tenia, o sin fin si recien empieza
        LocalDateTime hasta = asignado ? null : viaje.getFechaInicio();
        if (viajeDAO.choferConOtroEnCurso(viaje.getId(), idChofer, hora, hasta)) {
            return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.VIAJE_OTRO_EN_CURSO);
        }
        if (viajeDAO.vehiculoConOtroEnCurso(viaje.getId(), viaje.getIdVehiculo(), hora, hasta)) {
            return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.VEHICULO_OTRO_VIAJE_EN_CURSO,
                    viaje.getMatricula());
        }
        if (asignado) {
            viajeDAO.cambiarEstado(viaje.getId(), EstadoViaje.EN_CURSO, hora);
        } else {
            viajeDAO.corregirInicio(viaje.getId(), hora);
        }
        avisarCambioDeHorario(viaje);
        return registrar(evento, idChofer, ResultadoEvento.APLICADO, null);
    }

    private ResultadoEventoDto finalizar(EventoViajeDto evento, Long idChofer, ViajeDto viaje, LocalDateTime hora) {
        if (viaje.getEstado() != EstadoViaje.EN_CURSO) {
            return transicionInvalida(evento, idChofer, viaje);
        }
        if (hora.isBefore(viaje.getFechaInicio())) {
            return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.EVENTO_FUERA_DEL_VIAJE, hora.toString());
        }
        viajeDAO.cambiarEstado(viaje.getId(), EstadoViaje.FINALIZADO, hora);
        avisarCambioDeHorario(viaje);
        return registrar(evento, idChofer, ResultadoEvento.APLICADO, null);
    }

    // carga, descarga e incidente no cambian el estado: valen si ocurrieron dentro del viaje, aunque lleguen
    // despues del fin (por ejemplo, desde otro celular que estuvo sin conexion)
    private ResultadoEventoDto registrarDurante(EventoViajeDto evento, Long idChofer, ViajeDto viaje, LocalDateTime hora) {
        if (viaje.getEstado() == EstadoViaje.ASIGNADO) {
            return transicionInvalida(evento, idChofer, viaje);
        }
        boolean antes = hora.isBefore(viaje.getFechaInicio());
        boolean despues = viaje.getFechaFin() != null && hora.isAfter(viaje.getFechaFin());
        if (antes || despues) {
            return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.EVENTO_FUERA_DEL_VIAJE, hora.toString());
        }
        return registrar(evento, idChofer, ResultadoEvento.APLICADO, null);
    }

    // null si los datos estan bien; si no, el evento queda registrado como rechazado
    private ResultadoEventoDto validarDatos(EventoViajeDto evento, Long idChofer, GuiaDto guia) {
        String descripcion = evento.getDescripcion();
        if (evento.getTipo() == TipoEventoViaje.INCIDENTE && descripcion == null) {
            return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.EVENTO_DESCRIPCION_REQUERIDA);
        }
        if (descripcion != null && descripcion.length() > EventoViaje.DESCRIPCION_MAXIMA) {
            // se guarda recortada: la columna no admite mas
            evento.setDescripcion(descripcion.substring(0, EventoViaje.DESCRIPCION_MAXIMA));
            return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.EVENTO_DESCRIPCION_LARGA,
                    String.valueOf(EventoViaje.DESCRIPCION_MAXIMA));
        }
        if (!llevaKilos(evento.getTipo())) {
            return null;
        }
        Integer kilos = evento.getKilos();
        if (kilos == null) {
            return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.EVENTO_KILOS_REQUERIDOS);
        }
        if (kilos <= 0 || kilos > guia.getVolumen()) {
            return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.EVENTO_KILOS_INVALIDOS,
                    String.valueOf(kilos), String.valueOf(guia.getVolumen()));
        }
        return null;
    }

    // una descripcion en blanco cuenta como que no hay; los kilos solo se guardan donde corresponden
    private static void normalizar(EventoViajeDto evento) {
        String descripcion = evento.getDescripcion();
        evento.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.strip());
        if (!llevaKilos(evento.getTipo())) {
            evento.setKilos(null);
        }
    }

    private static boolean llevaKilos(TipoEventoViaje tipo) {
        return tipo == TipoEventoViaje.CARGA_PARCIAL || tipo == TipoEventoViaje.DESCARGA_PARCIAL;
    }

    // con la nueva hora de inicio o de fin se reasocian las posiciones del vehiculo, al confirmarse (CU-10)
    private void avisarCambioDeHorario(ViajeDto viaje) {
        inicioOFin.fire(new ViajeIniciadoOFinalizado(viaje.getId()));
    }

    private ResultadoEventoDto transicionInvalida(EventoViajeDto evento, Long idChofer, ViajeDto viaje) {
        return registrar(evento, idChofer, ResultadoEvento.RECHAZADO, CodigoError.VIAJE_TRANSICION_INVALIDA,
                viaje.getEstado().name(), evento.getTipo().name());
    }

    private boolean fechaValida(Instant fecha, GuiaDto guia) {
        Instant inicioGuia = guia.getFecha().atStartOfDay(ZONA).toInstant();
        return !fecha.isBefore(inicioGuia) && !fecha.isAfter(Instant.now().plus(TOLERANCIA_FUTURO));
    }

    private ResultadoEventoDto registrar(EventoViajeDto evento, Long idChofer, ResultadoEvento resultado,
            CodigoError codigo, String... parametros) {
        String mensaje = codigo == null ? null : MensajesError.resolver(codigo, parametros);
        eventoDAO.registrar(evento, idChofer, resultado, mensaje);
        return new ResultadoEventoDto(evento.getIdEvento(), resultado, mensaje);
    }
}
