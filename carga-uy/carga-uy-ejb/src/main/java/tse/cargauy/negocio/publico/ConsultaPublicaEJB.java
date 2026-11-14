package tse.cargauy.negocio.publico;

import java.time.LocalDate;
import java.util.List;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.informe.InformeDAOLocal;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.HabilitacionEmpresaDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
@PermitAll
public class ConsultaPublicaEJB implements ConsultaPublicaEJBLocal {

    @EJB
    EmpresaDAOLocal empresaDAO;

    @EJB
    InformeDAOLocal informeDAO;

    @Override
    public HabilitacionEmpresaDto validarEmpresa(Integer nroEmpresa, String razonSocial) {
        boolean porRazonSocial = razonSocial != null && !razonSocial.isBlank();
        if ((nroEmpresa == null) == !porRazonSocial) {
            throw new CargaUYException(CodigoError.EMPRESA_CONSULTA_DATO_REQUERIDO);
        }
        EmpresaDto empresa = porRazonSocial ? porRazonSocial(razonSocial) : empresaDAO.getEmpresaByNro(nroEmpresa);
        if (empresa == null) {
            throw new CargaUYException(CodigoError.EMPRESA_CONSULTA_SIN_RESULTADO);
        }
        LocalDate hoy = hoy();
        return new HabilitacionEmpresaDto(empresa.getNombrePublico(), empresa.getRazonSocial(),
                informeDAO.isEmpresaHabilitada(empresa.getId(), hoy), hoy);
    }

    private EmpresaDto porRazonSocial(String razonSocial) {
        List<EmpresaDto> empresas = empresaDAO.getByRazonSocial(razonSocial, 2);
        if (empresas.size() > 1) {
            throw new CargaUYException(CodigoError.EMPRESA_CONSULTA_AMBIGUA);
        }
        return empresas.isEmpty() ? null : empresas.get(0);
    }

    LocalDate hoy() {
        return LocalDate.now();
    }
}
