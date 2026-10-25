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
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.validacion.Cedula;

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

    // choferes vigentes a la fecha, para elegir a quien asignarle un viaje
    @RolesAllowed({ Rol.FUNCIONARIO, Rol.RESPONSABLE })
    public PaginaDto<UsuarioDto> getChoferes(Long idEmpresa, String cedula, LocalDate fecha, Paginacion paginacion) {
        validarAcceso(idEmpresa);
        return usuarioDAO.getChoferes(idEmpresa, cedula, fecha == null ? LocalDate.now() : fecha, paginacion);
    }

    public EmpresaDto addEmpresa(EmpresaDto empresaDto) {
        validar(empresaDto);
        String cedula = empresaDto.getCedulaResponsable();
        validarResponsable(cedula);
        if (empresaDAO.getEmpresaByNro(empresaDto.getNroEmpresa()) != null) {
            throw new CargaUYException(CodigoError.EMPRESA_NRO_DUPLICADO, String.valueOf(empresaDto.getNroEmpresa()));
        }

        empresaDto.setFechaAlta(LocalDate.now());
        EmpresaDto empresa = empresaDAO.addEmpresa(empresaDto);

        UsuarioDto ciudadano = usuarioDAO.getCiudadanoByCedula(cedula);
        if (ciudadano == null) {
            ciudadano = usuarioDAO.addCiudadano(cedula, null);
        }
        usuarioDAO.addResponsable(ciudadano.getId(), empresa.getId(), empresa.getFechaAlta());
        empresa.setCedulaResponsable(cedula);
        return empresa;
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

    private void validarResponsable(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            throw new CargaUYException(CodigoError.EMPRESA_RESPONSABLE_REQUERIDO);
        }
        if (!Cedula.esValida(cedula)) {
            throw new CargaUYException(CodigoError.CEDULA_INVALIDA, cedula);
        }
        if (usuarioDAO.getFuncionarioByCedula(cedula) != null) {
            throw new CargaUYException(CodigoError.EMPRESA_RESPONSABLE_FUNCIONARIO, cedula);
        }
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
