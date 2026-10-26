package tse.cargauy.negocio.nodo;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import tse.cargauy.data.nodo.NodoPerifericoDAOLocal;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.Rol;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.seguridad.Cifrado;

// CU-19. El administrador habilita el nodo sin una verificacion previa de conectividad: la letra no la pide y la
// integracion con tracking es one-way, el central no le pregunta nada al nodo
@Stateless
@RolesAllowed(Rol.ADMINISTRADOR)
public class NodoPerifericoEJB implements NodoPerifericoEJBLocal {

    @EJB
    NodoPerifericoDAOLocal nodoDAO;

    // la clave con la que el central consulta al nodo se guarda cifrada y no hasheada como las de las balanzas:
    // el central la tiene que mandar en cada consulta, asi que necesita poder recuperarla.
    // el identificador del nodo va como dato asociado: si alguien copia la clave cifrada de un nodo a otro en la
    // base, al descifrarla falla
    @Inject
    Cifrado cifrado;

    public List<NodoPerifericoDto> getVigentes() {
        return nodoDAO.getVigentes();
    }

    public NodoPerifericoDto getById(Long id) {
        NodoPerifericoDto nodo = nodoDAO.getById(id);
        if (nodo == null) {
            throw new CargaUYException(CodigoError.NODO_NO_ENCONTRADO, String.valueOf(id));
        }
        return nodo;
    }

    public void addNodo(NodoPerifericoDto nodoDto) {
        if (nodoDto.getIdentificador() == null || nodoDto.getIdentificador().isBlank()) {
            throw new CargaUYException(CodigoError.NODO_IDENTIFICADOR_REQUERIDO);
        }
        if (nodoDto.getTipo() == null) {
            throw new CargaUYException(CodigoError.NODO_TIPO_REQUERIDO);
        }
        if (nodoDAO.getByIdentificador(nodoDto.getIdentificador()) != null) {
            throw new CargaUYException(CodigoError.NODO_IDENTIFICADOR_DUPLICADO, nodoDto.getIdentificador());
        }
        if (nodoDto.getTipo() == TipoNodo.BALANZAS && vacio(nodoDto.getClave())) {
            throw new CargaUYException(CodigoError.NODO_CLAVE_REQUERIDA);
        }
        // todo nodo nuevo arranca deshabilitado: un nodo mal configurado no da error, da silencio
        nodoDto.setEstado(EstadoNodo.DESHABILITADO);
        nodoDAO.addNodo(nodoDto, vacio(nodoDto.getClave()) ? null : cifrado.cifrar(nodoDto.getClave(), nodoDto.getIdentificador()));
    }

    public void habilitar(Long id) {
        nodoDAO.cambiarEstado(vigente(id).getId(), EstadoNodo.HABILITADO);
    }

    public void deshabilitar(Long id) {
        nodoDAO.cambiarEstado(vigente(id).getId(), EstadoNodo.DESHABILITADO);
    }

    public void cambiarPuntoAcceso(Long id, String puntoAcceso) {
        NodoPerifericoDto nodo = vigente(id);
        nodoDAO.cambiarPuntoAcceso(nodo.getId(), puntoAcceso);
        nodoDAO.cambiarEstado(nodo.getId(), EstadoNodo.DESHABILITADO);
    }

    public void cambiarClave(Long id, String clave) {
        if (vacio(clave)) {
            throw new CargaUYException(CodigoError.NODO_CLAVE_REQUERIDA);
        }
        NodoPerifericoDto nodo = vigente(id);
        nodoDAO.cambiarClave(nodo.getId(), cifrado.cifrar(clave, nodo.getIdentificador()));
    }

    // baja logica: las posiciones y pesajes que reporto siguen siendo evidencia de casos
    public void darDeBaja(Long id) {
        nodoDAO.cambiarEstado(vigente(id).getId(), EstadoNodo.BAJA);
    }

    private NodoPerifericoDto vigente(Long id) {
        NodoPerifericoDto nodo = getById(id);
        if (nodo.getEstado() == EstadoNodo.BAJA) {
            throw new CargaUYException(CodigoError.NODO_DADO_DE_BAJA, nodo.getIdentificador());
        }
        return nodo;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
