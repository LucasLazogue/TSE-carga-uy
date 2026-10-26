package tse.cargauy.negocio.fiscalizacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import tse.cargauy.data.fiscalizacion.CasoFiscalizacionDAOLocal;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.posicion.EventoPosicionDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.EventoPosicionDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PesajeDto;
import tse.cargauy.dtos.PesajesViajeDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.TipoEvidencia;
import tse.cargauy.entities.TipoInfraccion;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.balanzas.BalanzasEJBLocal;

// cada caso se registra en su propia transaccion: si una regla falla no se pierde el caso de otra
@Stateless
@PermitAll
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MotorFiscalizacionEJB implements MotorFiscalizacionEJBLocal {

    // los viajes y los permisos usan la hora local; la zona es la del servidor (CARGAUY_ZONA_HORARIA)
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private static final Logger LOGGER = Logger.getLogger(MotorFiscalizacionEJB.class.getName());

    @EJB
    EventoPosicionDAOLocal eventoDAO;

    @EJB
    ViajeDAOLocal viajeDAO;

    @EJB
    PermisoDAOLocal permisoDAO;

    @EJB
    CasoFiscalizacionEJBLocal casoEJB;

    @EJB
    CasoFiscalizacionDAOLocal casoDAO;

    @EJB
    VehiculoDAOLocal vehiculoDAO;

    @EJB
    BalanzasEJBLocal balanzas;

    @Asynchronous
    public void evaluarPosicion(Long idNodo, String idEvento) {
        EventoPosicionDto posicion = eventoDAO.get(idNodo, idEvento);
        Long idVehiculo = posicion.getIdVehiculo();
        if (idVehiculo == null) {
            return;
        }
        LocalDateTime momento = LocalDateTime.ofInstant(posicion.getTimestampGeneracion(), ZONA);
        String origen = posicion.getNodo() + "/" + posicion.getIdEvento();

        if (viajeDAO.getEnCursoDelVehiculo(idVehiculo, momento) == null && !tieneViajeAsignado(idVehiculo)) {
            CasoFiscalizacionDto caso = new CasoFiscalizacionDto(TipoInfraccion.VIAJE_SIN_GUIA,
                    clave(TipoInfraccion.VIAJE_SIN_GUIA, idVehiculo, momento.toLocalDate()), momento, idVehiculo, null);
            registrar(caso, List.of(new EvidenciaDto(TipoEvidencia.POSICION, origen, momento,
                    posicion.getLatitud() + ", " + posicion.getLongitud(), "sin viaje en curso", null,
                    "El vehiculo circulaba sin un viaje en curso.")));
        }
        evaluarPermiso(idVehiculo, momento, null, origen);
    }

    @Asynchronous
    public void evaluarViaje(Long idViaje) {
        ViajeDto viaje = viajeDAO.getViajeById(idViaje);
        if (viaje.getEstado() != EstadoViaje.FINALIZADO) {
            return;
        }
        evaluarPermiso(viaje.getIdVehiculo(), viaje.getFechaInicio(), idViaje, "viaje " + idViaje);
        evaluarCarga(viaje);
    }

    // cada minuto vuelve a evaluar los viajes a los que les falto la respuesta de alguna balanza
    @Schedule(minute = "*", hour = "*", persistent = false)
    public void reevaluarCargasPendientes() {
        LocalDateTime ahora = ahora();
        for (Long idViaje : viajeDAO.reservarEvaluacionesCarga(ahora, ahora.plusMinutes(1), 20)) {
            try {
                evaluarCarga(viajeDAO.getViajeById(idViaje));
            } catch (RuntimeException e) {
                LOGGER.warning("fiscalizacion evaluacion=FALLIDA viaje=" + idViaje + " error=" + e);
            }
        }
    }

    private void evaluarPermiso(Long idVehiculo, LocalDateTime momento, Long idViaje, String origen) {
        LocalDate fecha = momento.toLocalDate();
        if (permisoDAO.getVigente(idVehiculo, fecha) != null) {
            return;
        }
        CasoFiscalizacionDto caso = new CasoFiscalizacionDto(TipoInfraccion.SIN_HABILITACION,
                clave(TipoInfraccion.SIN_HABILITACION, idVehiculo, fecha), momento, idVehiculo, idViaje);
        registrar(caso, List.of(new EvidenciaDto(TipoEvidencia.PERMISO, origen, momento, "sin permiso aprobado vigente",
                fecha.toString(), null, "El vehiculo circulaba sin un permiso vigente.")));
    }

    // un caso por viaje, porque el viaje puede durar mas de un dia. si alguna balanza no respondio se evalua con lo
    // que llego y queda pendiente: lo que traiga despues se agrega al mismo caso
    private void evaluarCarga(ViajeDto viaje) {
        PesajesViajeDto resultado = balanzas.getPesajesDelViaje(viaje);
        VehiculoDto vehiculo = vehiculoDAO.getVehiculoById(viaje.getIdVehiculo());
        int maximo = vehiculo.getPesoVehiculo() + vehiculo.getCapacidadCarga();
        List<EvidenciaDto> evidencias = new ArrayList<>();
        for (PesajeDto pesaje : resultado.getPesajes()) {
            if (pesaje.getPesoRegistrado() > maximo) {
                evidencias.add(new EvidenciaDto(TipoEvidencia.PESAJE, pesaje.getIdOrigen(), pesaje.getFechaHora(),
                        pesaje.getPesoRegistrado() + " kg", maximo + " kg", "peso del vehiculo + capacidad declarada",
                        "Pesada en " + pesaje.getNombrePeaje() + " por encima del maximo."));
            }
        }
        if (!evidencias.isEmpty()) {
            CasoFiscalizacionDto caso = new CasoFiscalizacionDto(TipoInfraccion.EXCESO_CARGA,
                    TipoInfraccion.EXCESO_CARGA + "|" + viaje.getIdVehiculo() + "|viaje-" + viaje.getId(),
                    evidencias.get(0).getFechaEvento(), viaje.getIdVehiculo(), viaje.getId());
            CasoFiscalizacionDto registrado = registrar(caso, evidencias);
            if (registrado != null) {
                casoDAO.agregarEvidencias(registrado.getId(), evidencias);
            }
        }
        viajeDAO.marcarEvaluacionCarga(viaje.getId(), resultado.isCompleto() ? null : ahora().plusMinutes(1));
    }

    // si el inicio del viaje todavia no llego desde el movil, el viaje sigue asignado y no es un caso
    private boolean tieneViajeAsignado(Long idVehiculo) {
        FiltroViajes filtro = new FiltroViajes(null, idVehiculo, EstadoViaje.ASIGNADO);
        return viajeDAO.getAll(filtro, new Paginacion(0, 1)).getTotal() > 0;
    }

    // un caso por vehiculo, regla y dia
    private static String clave(TipoInfraccion tipo, Long idVehiculo, LocalDate fecha) {
        return tipo + "|" + idVehiculo + "|" + fecha;
    }

    // devuelve el caso nuevo o el que ya existia, o null si no se pudo registrar
    private CasoFiscalizacionDto registrar(CasoFiscalizacionDto caso, List<EvidenciaDto> evidencias) {
        try {
            return casoEJB.registrarCaso(caso, evidencias);
        } catch (CargaUYException e) {
            // CASO_YA_REGISTRADO: otra posicion del mismo vehiculo registro el caso al mismo tiempo.
            // Los otros errores se loguean aca porque en segundo plano no le llegan a nadie
            if (e.getCodigo() != CodigoError.CASO_YA_REGISTRADO) {
                LOGGER.warning("fiscalizacion evaluacion=FALLIDA caso=" + caso.getClaveDeduplicacion() + " error=" + e);
            }
            return null;
        }
    }

    // package-private para sobrescribirla en los tests
    LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
