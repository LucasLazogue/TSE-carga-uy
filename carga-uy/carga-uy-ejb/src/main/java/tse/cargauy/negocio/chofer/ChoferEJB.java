package tse.cargauy.negocio.chofer;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.ChoferDto;
import tse.cargauy.dtos.FiltroChoferes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;
import tse.cargauy.validacion.Cedula;

@Stateless
@RolesAllowed({ Rol.FUNCIONARIO, Rol.RESPONSABLE })
public class ChoferEJB implements ChoferEJBLocal, ChoferEJBRemote {

    @EJB
    UsuarioDAOLocal usuarioDAO;

    @EJB
    ViajeDAOLocal viajeDAO;

    @EJB
    EmpresaDAOLocal empresaDAO;

    @EJB
    EmpresaEJBLocal empresaEJB;

    public PaginaDto<ChoferDto> getChoferes(Long idEmpresa, FiltroChoferes filtro, Paginacion paginacion) {
        empresaEJB.validarAcceso(idEmpresa);
        if (filtro.getFecha() == null) {
            filtro.setFecha(LocalDate.now());
        }
        return usuarioDAO.getChoferes(idEmpresa, filtro, paginacion);
    }

    public ChoferDto addChofer(Long idEmpresa, ChoferDto choferDto) {
        empresaEJB.validarAcceso(idEmpresa);
        if (empresaDAO.getEmpresaById(idEmpresa) == null) {
            throw new CargaUYException(CodigoError.EMPRESA_NO_ENCONTRADA, String.valueOf(idEmpresa));
        }
        validar(choferDto);

        String cedula = choferDto.getCedula();
        LocalDate hoy = LocalDate.now();
        UsuarioDto ciudadano = usuarioDAO.getCiudadanoByCedula(cedula);
        if (ciudadano == null) {
            ciudadano = usuarioDAO.addCiudadano(cedula, null);
        } else if (usuarioDAO.getChoferSinBaja(ciudadano.getId(), idEmpresa, hoy) != null) {
            throw new CargaUYException(CodigoError.CHOFER_YA_ACTIVO, cedula);
        }
        return usuarioDAO.addChofer(ciudadano.getId(), idEmpresa, hoy);
    }

    public ChoferDto darDeBaja(Long idEmpresa, Long idVinculo) {
        empresaEJB.validarAcceso(idEmpresa);
        ChoferDto chofer = usuarioDAO.getVinculoSinBaja(idVinculo, idEmpresa);
        if (chofer == null) {
            throw new CargaUYException(CodigoError.CHOFER_NO_ENCONTRADO, String.valueOf(idVinculo));
        }
        List<ViajeDto> pendientes = viajeDAO.getPendientesDelVinculo(idVinculo);
        if (!pendientes.isEmpty()) {
            String guias = pendientes.stream()
                    .map(v -> v.getNroGuia() + " " + v.getEstado())
                    .collect(Collectors.joining(", "));
            throw new CargaUYException(CodigoError.CHOFER_CON_VIAJES_PENDIENTES, chofer.getCedula(), guias);
        }
        return usuarioDAO.darDeBajaChofer(idVinculo, LocalDate.now());
    }

    private void validar(ChoferDto choferDto) {
        if (choferDto == null || choferDto.getCedula() == null || choferDto.getCedula().isBlank()) {
            throw new CargaUYException(CodigoError.CHOFER_CEDULA_REQUERIDA);
        }
        if (!Cedula.esValida(choferDto.getCedula())) {
            throw new CargaUYException(CodigoError.CEDULA_INVALIDA, choferDto.getCedula());
        }
        if (usuarioDAO.getFuncionarioByCedula(choferDto.getCedula()) != null) {
            throw new CargaUYException(CodigoError.CHOFER_FUNCIONARIO, choferDto.getCedula());
        }
    }
}
