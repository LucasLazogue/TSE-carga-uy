package tse.cargauy.negocio.movil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.EventoViajeDto;
import tse.cargauy.dtos.ResultadoEventoDto;
import tse.cargauy.dtos.SyncRespuestaDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.dtos.ViajeMovilDto;
import tse.cargauy.entities.Rol;

@Stateless
@RolesAllowed(Rol.CHOFER)
public class ViajeChoferEJB implements ViajeChoferEJBLocal, ViajeChoferEJBRemote {

    @EJB
    ViajeDAOLocal viajeDAO;

    @EJB
    GuiaDAOLocal guiaDAO;

    @EJB
    AplicadorEventosEJBLocal aplicador;

    @Resource
    SessionContext contexto;

    public ViajeMovilDto getViajeActual() {
        ViajeDto viaje = viajeDAO.getActualDelChofer(idUsuario());
        return viaje == null ? null : new ViajeMovilDto(viaje, guiaDAO.getGuiaById(viaje.getIdGuia()));
    }

    // se aplican en el orden en que se generaron en el celular, no en el que llegaron
    public SyncRespuestaDto sincronizar(List<EventoViajeDto> eventos) {
        List<EventoViajeDto> ordenados = new ArrayList<>(eventos);
        ordenados.sort(Comparator.comparing(EventoViajeDto::getTimestampGeneracion));
        Long idChofer = idUsuario();
        List<ResultadoEventoDto> resultados = new ArrayList<>();
        for (EventoViajeDto evento : ordenados) {
            resultados.add(aplicador.aplicar(evento, idChofer));
        }
        return new SyncRespuestaDto(resultados, getViajeActual());
    }

    // package-private para sobrescribirlo en los tests
    Long idUsuario() {
        return Long.valueOf(contexto.getCallerPrincipal().getName());
    }
}
