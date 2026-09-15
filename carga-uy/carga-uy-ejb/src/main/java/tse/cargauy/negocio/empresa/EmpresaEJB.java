package tse.cargauy.negocio.empresa;

import java.time.LocalDate;
import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.exceptions.CargaUYException;

@Stateless
public class EmpresaEJB implements EmpresaEJBLocal, EmpresaEJBRemote {

    @EJB
    private EmpresaDAOLocal empresaDAO;

    public EmpresaDto getEmpresaById(Long id) {
        return empresaDAO.getEmpresaById(id);
    }

    public List<EmpresaDto> getAll() {
        return empresaDAO.getAll();
    }

    public List<EmpresaDto> findByNombre(String nombre) {
        return empresaDAO.findByNombre(nombre);
    }

    public void addEmpresa(EmpresaDto empresaDto) {
        validar(empresaDto);
        if (empresaDAO.getEmpresaByNro(empresaDto.getNroEmpresa()) != null) {
            throw new CargaUYException("Ya existe una empresa con el numero " + empresaDto.getNroEmpresa() + ".");
        }

        empresaDto.setFechaAlta(LocalDate.now());
        empresaDAO.addEmpresa(empresaDto);
    }

    public void updateEmpresa(Long id, EmpresaDto empresaDto) {
        validar(empresaDto);
        EmpresaDto existente = empresaDAO.getEmpresaByNro(empresaDto.getNroEmpresa());
        if (existente != null && !existente.getId().equals(id)) {
            throw new CargaUYException("Ya existe una empresa con el numero " + empresaDto.getNroEmpresa() + ".");
        }

        empresaDAO.updateEmpresa(id, empresaDto);
    }

    public void deleteEmpresa(Long id) {
        empresaDAO.deleteEmpresa(id);
    }

    private void validar(EmpresaDto empresaDto) {
        if (empresaDto.getNroEmpresa() <= 0) {
            throw new CargaUYException("El numero de empresa debe ser mayor a 0.");
        }
        if (empresaDto.getNombrePublico() == null || empresaDto.getNombrePublico().isBlank()) {
            throw new CargaUYException("El nombre publico es obligatorio.");
        }
        if (empresaDto.getRazonSocial() == null || empresaDto.getRazonSocial().isBlank()) {
            throw new CargaUYException("La razon social es obligatoria.");
        }
    }

}
