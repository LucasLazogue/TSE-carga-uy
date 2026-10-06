package tse.cargauy.negocio.nodo;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import tse.cargauy.data.nodo.NodoPerifericoDAOLocal;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

// CU-19. Falta la verificacion de conectividad antes de habilitar (paso 5 de la ficha): para un nodo de tracking
// no esta decidido como hacerla, porque la integracion es one-way y el central no le puede preguntar nada
@Stateless
public class NodoPerifericoEJB implements NodoPerifericoEJBLocal {

    @EJB
    NodoPerifericoDAOLocal nodoDAO;

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
        // todo nodo nuevo arranca deshabilitado: un nodo mal configurado no da error, da silencio
        nodoDto.setEstado(EstadoNodo.DESHABILITADO);
        nodoDAO.addNodo(nodoDto);
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
}
