package tse.cargauy.negocio.tracking;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import tse.cargauy.data.posicion.EventoPosicionDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.ViajeDto;

// CU-10: cada posicion es del viaje que el vehiculo tenia en curso cuando se genero. El inicio y el fin pueden llegar
// tarde desde el movil, despues que las posiciones: cuando llegan, se recalcula el viaje de las posiciones del
// vehiculo desde que empezo ese viaje. Las que quedan sin viaje son las que evalua la regla de viaje sin Guia
@Stateless
public class ReasociadorPosiciones {

    // los viajes guardan la hora local sin zona; la zona es la del servidor (CARGAUY_ZONA_HORARIA)
    private static final ZoneId ZONA = ZoneId.systemDefault();

    @EJB
    ViajeDAOLocal viajeDAO;

    @EJB
    EventoPosicionDAOLocal eventoDAO;

    // se llama con la transaccion que cambio el viaje ya confirmada: cada consulta usa la suya
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void reasociar(Long idViaje) {
        ViajeDto viaje = viajeDAO.getViajeById(idViaje);
        if (viaje == null || viaje.getFechaInicio() == null) {
            return;
        }
        List<Intervalo> viajes = new ArrayList<>();
        for (ViajeDto v : viajeDAO.getIniciadosDelVehiculo(viaje.getIdVehiculo(), viaje.getFechaInicio())) {
            viajes.add(Intervalo.de(v));
        }
        for (Tramo tramo : tramos(viajes, instante(viaje.getFechaInicio()))) {
            eventoDAO.asociarAViaje(viaje.getMatricula(), viaje.getIdVehiculo(), tramo.desde(), tramo.hasta(),
                    tramo.idViaje());
        }
    }

    // el periodo en que la posicion es de ese viaje: [inicio, finExclusivo). finExclusivo null = sigue en curso
    record Intervalo(Long idViaje, Instant inicio, Instant finExclusivo) {
        static Intervalo de(ViajeDto viaje) {
            // MongoDB guarda milisegundos: una posicion generada en el mismo milisegundo del fin es del viaje
            Instant fin = viaje.getFechaFin() == null ? null
                    : instante(viaje.getFechaFin()).truncatedTo(ChronoUnit.MILLIS).plusMillis(1);
            return new Intervalo(viaje.getId(), instante(viaje.getFechaInicio()), fin);
        }

        boolean cubre(Instant momento) {
            return !momento.isBefore(inicio) && (finExclusivo == null || momento.isBefore(finExclusivo));
        }
    }

    // las posiciones generadas en [desde, hasta) son del viaje idViaje; hasta null = sin limite, idViaje null = ninguno
    record Tramo(Instant desde, Instant hasta, Long idViaje) {
    }

    // parte el tiempo desde 'desde' en tramos con un solo viaje cada uno. Si dos viajes se superponen, porque uno
    // quedo sin fin registrado, el tramo es del que empezo ultimo
    static List<Tramo> tramos(List<Intervalo> viajes, Instant desde) {
        TreeSet<Instant> cortes = new TreeSet<>();
        cortes.add(desde);
        for (Intervalo v : viajes) {
            if (v.inicio().isAfter(desde)) {
                cortes.add(v.inicio());
            }
            if (v.finExclusivo() != null && v.finExclusivo().isAfter(desde)) {
                cortes.add(v.finExclusivo());
            }
        }
        List<Instant> lista = new ArrayList<>(cortes);
        List<Tramo> tramos = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            Instant inicioTramo = lista.get(i);
            Instant finTramo = i + 1 < lista.size() ? lista.get(i + 1) : null;
            Long idViaje = elQueEmpezoUltimo(viajes, inicioTramo);
            Tramo anterior = tramos.isEmpty() ? null : tramos.get(tramos.size() - 1);
            if (anterior != null && Objects.equals(anterior.idViaje(), idViaje)) {
                tramos.set(tramos.size() - 1, new Tramo(anterior.desde(), finTramo, idViaje));
            } else {
                tramos.add(new Tramo(inicioTramo, finTramo, idViaje));
            }
        }
        return tramos;
    }

    private static Long elQueEmpezoUltimo(List<Intervalo> viajes, Instant momento) {
        Intervalo elegido = null;
        for (Intervalo v : viajes) {
            if (v.cubre(momento) && (elegido == null || v.inicio().isAfter(elegido.inicio()))) {
                elegido = v;
            }
        }
        return elegido == null ? null : elegido.idViaje();
    }

    private static Instant instante(LocalDateTime fecha) {
        return fecha.atZone(ZONA).toInstant();
    }
}
