package tse.cargauy.nodobalanzas.negocio.pesaje;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import tse.cargauy.nodobalanzas.data.pesaje.PesajeDAOLocal;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;
import tse.cargauy.nodobalanzas.exceptions.CodigoError;
import tse.cargauy.nodobalanzas.exceptions.NodoBalanzasException;
import tse.cargauy.nodobalanzas.seguridad.Rol;

@Stateless
public class PesajeEJB implements PesajeEJBLocal {

    @EJB
    PesajeDAOLocal pesajeDAO;

    @Resource
    SessionContext contexto;

    @RolesAllowed(Rol.CENTRAL)
    public List<PesajeDto> getPesajes(String matricula, LocalDate fecha) {
        if (vacio(matricula)) {
            throw new NodoBalanzasException(CodigoError.PESAJE_MATRICULA_REQUERIDA);
        }
        if (fecha == null) {
            throw new NodoBalanzasException(CodigoError.PESAJE_FECHA_REQUERIDA);
        }
        return pesajeDAO.getPesajesByMatricula(matricula, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
    }

    @RolesAllowed(Rol.BALANZA)
    public void addPesajes(List<PesajeDto> lecturas) {
        if (lecturas == null || lecturas.isEmpty()) {
            throw new NodoBalanzasException(CodigoError.PESAJE_LECTURAS_REQUERIDAS);
        }
        for (PesajeDto lectura : lecturas) {
            validar(lectura);
        }
        String balanza = codigoBalanza();
        LocalDateTime recibidoEn = ahora();
        for (PesajeDto lectura : lecturas) {
            if (!pesajeDAO.existePesaje(balanza, lectura.getIdRegistro())) {
                pesajeDAO.addPesaje(balanza, lectura, recibidoEn);
            }
        }
    }

    String codigoBalanza() {
        return contexto.getCallerPrincipal().getName();
    }

    LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static void validar(PesajeDto lectura) {
        if (vacio(lectura.getIdRegistro())) {
            throw new NodoBalanzasException(CodigoError.PESAJE_REGISTRO_REQUERIDO);
        }
        if (vacio(lectura.getMatricula())) {
            throw new NodoBalanzasException(CodigoError.PESAJE_MATRICULA_REQUERIDA);
        }
        if (lectura.getFechaHora() == null) {
            throw new NodoBalanzasException(CodigoError.PESAJE_FECHA_HORA_REQUERIDA);
        }
        if (lectura.getPesoRegistrado() <= 0) {
            throw new NodoBalanzasException(CodigoError.PESAJE_PESO_INVALIDO);
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
