package tse.cargauy.nodobalanzas.negocio.pesaje;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import tse.cargauy.nodobalanzas.data.pesaje.PesajeDAOLocal;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;
import tse.cargauy.nodobalanzas.exceptions.CodigoError;
import tse.cargauy.nodobalanzas.exceptions.NodoBalanzasException;
import tse.cargauy.nodobalanzas.seguridad.Rol;

@Stateless
@RolesAllowed(Rol.CENTRAL)
public class PesajeEJB implements PesajeEJBLocal {

    @EJB
    PesajeDAOLocal pesajeDAO;

    public List<PesajeDto> getPesajes(String matricula, String rut, LocalDate fecha) {
        if (fecha == null) {
            throw new NodoBalanzasException(CodigoError.PESAJE_FECHA_REQUERIDA);
        }
        boolean porMatricula = matricula != null && !matricula.isBlank();
        boolean porRut = rut != null && !rut.isBlank();
        if (porMatricula == porRut) {
            throw new NodoBalanzasException(CodigoError.PESAJE_MATRICULA_O_RUT);
        }
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();
        if (porMatricula) {
            return pesajeDAO.getPesajesByMatricula(matricula, desde, hasta);
        }
        return pesajeDAO.getPesajesByRut(rut, desde, hasta);
    }
}
