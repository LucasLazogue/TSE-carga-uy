package tse.cargauy.negocio.fiscalizacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import tse.cargauy.data.fiscalizacion.CasoFiscalizacionDAOLocal;
import tse.cargauy.dtos.NormaFiscalizacionDto;
import tse.cargauy.entities.NormaFiscalizacion;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

// CU-17. Cambiar un valor agrega una version con su fecha de vigencia en lugar de pisar la anterior: cada hecho se
// evalua con la norma vigente a su fecha
@Stateless
@RolesAllowed(Rol.ADMINISTRADOR)
public class NormaFiscalizacionEJB implements NormaFiscalizacionEJBLocal {

    private static final Logger LOGGER = Logger.getLogger(NormaFiscalizacionEJB.class.getName());

    @EJB
    CasoFiscalizacionDAOLocal casoDAO;

    @Resource
    SessionContext contexto;

    public List<NormaFiscalizacionDto> getVigentes() {
        LocalDate hoy = hoy();
        return NormaFiscalizacion.CLAVES.stream()
                .map(clave -> casoDAO.getNormas(clave).stream()
                        .filter(n -> !n.getVigenteDesde().isAfter(hoy))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<NormaFiscalizacionDto> getHistorial(String clave) {
        validarClave(clave);
        return casoDAO.getNormas(clave);
    }

    public void cambiarValor(String clave, int valor, LocalDate vigenteDesde) {
        validarClave(clave);
        if (valor < 0) {
            throw new CargaUYException(CodigoError.NORMA_VALOR_INVALIDO);
        }
        if (vigenteDesde == null || vigenteDesde.isBefore(hoy())) {
            throw new CargaUYException(CodigoError.NORMA_VIGENCIA_INVALIDA);
        }
        casoDAO.addNorma(clave, valor, vigenteDesde);
        LOGGER.info("norma cambio usuario=" + contexto.getCallerPrincipal().getName() + " clave=" + clave + " valor=" + valor
                + " vigenteDesde=" + vigenteDesde);
    }

    private static void validarClave(String clave) {
        if (clave == null || !NormaFiscalizacion.CLAVES.contains(clave)) {
            throw new CargaUYException(CodigoError.NORMA_NO_ENCONTRADA, String.valueOf(clave));
        }
    }

    LocalDate hoy() {
        return LocalDate.now();
    }
}
