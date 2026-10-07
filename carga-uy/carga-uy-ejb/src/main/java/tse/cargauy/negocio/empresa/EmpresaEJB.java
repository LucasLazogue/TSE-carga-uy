package tse.cargauy.negocio.empresa;

import java.time.LocalDate;
import java.util.List;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
public class EmpresaEJB implements EmpresaEJBLocal, EmpresaEJBRemote {

    @EJB
    EmpresaDAOLocal empresaDAO;

    public EmpresaDto getEmpresaById(Long id) {
        EmpresaDto empresa = empresaDAO.getEmpresaById(id);
        if (empresa == null) {
            throw new CargaUYException(CodigoError.EMPRESA_NO_ENCONTRADA, String.valueOf(id));
        }
        return empresa;
    }

    public List<EmpresaDto> getAll() {
        return empresaDAO.getAll();
    }

    public List<EmpresaDto> findByNombre(String nombre) {
        return empresaDAO.findByNombre(nombre);
    }

    public EmpresaDto addEmpresa(EmpresaDto empresaDto) {
        validar(empresaDto);
        if (empresaDAO.getEmpresaByNro(empresaDto.getNroEmpresa()) != null) {
            throw new CargaUYException(CodigoError.EMPRESA_NRO_DUPLICADO, String.valueOf(empresaDto.getNroEmpresa()));
        }

        empresaDto.setFechaAlta(LocalDate.now());
        return empresaDAO.addEmpresa(empresaDto);
    }

    public void updateEmpresa(Long id, EmpresaDto empresaDto) {
        validar(empresaDto);
        EmpresaDto existente = empresaDAO.getEmpresaByNro(empresaDto.getNroEmpresa());
        if (existente != null && !existente.getId().equals(id)) {
            throw new CargaUYException(CodigoError.EMPRESA_NRO_DUPLICADO, String.valueOf(empresaDto.getNroEmpresa()));
        }

        empresaDAO.updateEmpresa(id, empresaDto);
    }

    public void deleteEmpresa(Long id) {
        empresaDAO.deleteEmpresa(id);
    }

    private void validar(EmpresaDto empresaDto) {
        if (empresaDto.getNroEmpresa() <= 0) {
            throw new CargaUYException(CodigoError.EMPRESA_NRO_INVALIDO);
        }
        if (empresaDto.getNombrePublico() == null || empresaDto.getNombrePublico().isBlank()) {
            throw new CargaUYException(CodigoError.EMPRESA_NOMBRE_PUBLICO_REQUERIDO);
        }
        if (empresaDto.getRazonSocial() == null || empresaDto.getRazonSocial().isBlank()) {
            throw new CargaUYException(CodigoError.EMPRESA_RAZON_SOCIAL_REQUERIDA);
        }
    }

}
