package tse.cargauy.negocio.empresa;

import java.time.LocalDate;
import java.util.List;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
@RolesAllowed(Rol.FUNCIONARIO)
public class EmpresaEJB implements EmpresaEJBLocal, EmpresaEJBRemote {

    @EJB
    EmpresaDAOLocal empresaDAO;

    @EJB
    UsuarioDAOLocal usuarioDAO;

    @Resource
    SessionContext contexto;

    @RolesAllowed({ Rol.FUNCIONARIO, Rol.RESPONSABLE })
    public EmpresaDto getEmpresaById(Long id) {
        validarAcceso(id);
        EmpresaDto empresa = empresaDAO.getEmpresaById(id);
        if (empresa == null) {
            throw new CargaUYException(CodigoError.EMPRESA_NO_ENCONTRADA, String.valueOf(id));
        }
        return empresa;
    }

    @RolesAllowed({ Rol.FUNCIONARIO, Rol.RESPONSABLE })
    public PaginaDto<EmpresaDto> getAll(Paginacion paginacion) {
        if (contexto.isCallerInRole(Rol.FUNCIONARIO)) {
            return empresaDAO.getAll(paginacion);
        }
        List<EmpresaDto> empresas = empresaDAO.getByResponsable(idUsuario(), LocalDate.now());
        return new PaginaDto<>(empresas, empresas.size(), 0, paginacion.getTamanio());
    }

    public PaginaDto<EmpresaDto> findByNombre(String nombre, Paginacion paginacion) {
        return empresaDAO.findByNombre(nombre, paginacion);
    }

    @RolesAllowed({ Rol.FUNCIONARIO, Rol.RESPONSABLE })
    public void validarAcceso(Long idEmpresa) {
        if (!contexto.isCallerInRole(Rol.FUNCIONARIO)
                && usuarioDAO.getResponsableVigente(idUsuario(), idEmpresa, LocalDate.now()) == null) {
            throw new CargaUYException(CodigoError.ACCESO_DENEGADO);
        }
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

    private Long idUsuario() {
        return Long.valueOf(contexto.getCallerPrincipal().getName());
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
