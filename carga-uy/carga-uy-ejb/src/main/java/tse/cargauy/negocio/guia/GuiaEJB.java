package tse.cargauy.negocio.guia;

import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
public class GuiaEJB implements GuiaEJBLocal, GuiaEJBRemote {

    @EJB
    GuiaDAOLocal guiaDAO;

    @EJB
    ViajeDAOLocal viajeDAO;

    @EJB
    EmpresaDAOLocal empresaDAO;

    @EJB
    UsuarioDAOLocal usuarioDAO;

    public GuiaDto getGuiaById(Long id) {
        return guiaDAO.getGuiaById(id);
    }

    public List<GuiaDto> getAll() {
        return guiaDAO.getAll();
    }

    public List<GuiaDto> getByEmpresa(Long idEmpresa) {
        return guiaDAO.getByEmpresa(idEmpresa);
    }

    public GuiaDto addGuia(GuiaDto guiaDto) {
        validar(guiaDto);
        if (guiaDto.getIdEmpresa() == null) {
            throw new CargaUYException(CodigoError.GUIA_EMPRESA_REQUERIDA);
        }
        if (empresaDAO.getEmpresaById(guiaDto.getIdEmpresa()) == null) {
            throw new CargaUYException(CodigoError.EMPRESA_NO_ENCONTRADA, String.valueOf(guiaDto.getIdEmpresa()));
        }
        UsuarioDto responsable = guiaDto.getIdRegistradaPor() == null ? null : usuarioDAO.getById(guiaDto.getIdRegistradaPor());
        if (responsable == null || !responsable.getRoles().contains(Rol.CIUDADANO)) {
            throw new CargaUYException(CodigoError.GUIA_RESPONSABLE_INVALIDO);
        }

        return guiaDAO.addGuia(guiaDto);
    }

    public void updateGuia(Long id, GuiaDto guiaDto) {
        GuiaDto actual = getExistente(id);
        ViajeDto viaje = viajeDAO.getByGuia(id);
        if (viaje != null && viaje.getEstado() != EstadoViaje.ASIGNADO) {
            throw new CargaUYException(CodigoError.GUIA_NO_MODIFICABLE, actual.getNroGuia());
        }
        validar(guiaDto);
        if (viaje != null && (!guiaDto.getFecha().equals(actual.getFecha()) || guiaDto.getVolumen() != actual.getVolumen())) {
            throw new CargaUYException(CodigoError.GUIA_ASIGNADA_DATOS_FIJOS, actual.getNroGuia());
        }

        guiaDAO.updateGuia(id, guiaDto);
    }

    public void deleteGuia(Long id) {
        GuiaDto actual = getExistente(id);
        if (viajeDAO.getByGuia(id) != null) {
            throw new CargaUYException(CodigoError.GUIA_CON_VIAJE, actual.getNroGuia());
        }

        guiaDAO.deleteGuia(id);
    }

    public List<RubroDto> getRubros() {
        return guiaDAO.getRubros();
    }

    public List<TipoCargaDto> getTiposCarga() {
        return guiaDAO.getTiposCarga();
    }

    private GuiaDto getExistente(Long id) {
        GuiaDto guia = guiaDAO.getGuiaById(id);
        if (guia == null) {
            throw new CargaUYException(CodigoError.GUIA_NO_ENCONTRADA, String.valueOf(id));
        }
        return guia;
    }

    private void validar(GuiaDto guiaDto) {
        if (guiaDto.getFecha() == null) {
            throw new CargaUYException(CodigoError.GUIA_FECHA_REQUERIDA);
        }
        if (guiaDto.getOrigenLat() == null || guiaDto.getOrigenLon() == null) {
            throw new CargaUYException(CodigoError.GUIA_ORIGEN_REQUERIDO);
        }
        if (guiaDto.getDestinoLat() == null || guiaDto.getDestinoLon() == null) {
            throw new CargaUYException(CodigoError.GUIA_DESTINO_REQUERIDO);
        }
        if (guiaDto.getOrigenLat().equals(guiaDto.getDestinoLat()) && guiaDto.getOrigenLon().equals(guiaDto.getDestinoLon())) {
            throw new CargaUYException(CodigoError.GUIA_ORIGEN_IGUAL_DESTINO);
        }
        if (guiaDto.getVolumen() <= 0) {
            throw new CargaUYException(CodigoError.GUIA_VOLUMEN_INVALIDO);
        }
        if (guiaDto.getIdRubro() == null) {
            throw new CargaUYException(CodigoError.GUIA_RUBRO_REQUERIDO);
        }
        if (guiaDAO.getRubroById(guiaDto.getIdRubro()) == null) {
            throw new CargaUYException(CodigoError.RUBRO_NO_ENCONTRADO, String.valueOf(guiaDto.getIdRubro()));
        }
        if (guiaDto.getIdTipoCarga() == null) {
            throw new CargaUYException(CodigoError.GUIA_TIPO_CARGA_REQUERIDO);
        }
        if (guiaDAO.getTipoCargaById(guiaDto.getIdTipoCarga()) == null) {
            throw new CargaUYException(CodigoError.TIPO_CARGA_NO_ENCONTRADO, String.valueOf(guiaDto.getIdTipoCarga()));
        }
    }

}
