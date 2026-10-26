package tse.cargauy.data.nodo;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.NodoPeriferico;
import tse.cargauy.entities.TipoNodo;

@Stateless
public class NodoPerifericoDAO implements NodoPerifericoDAOLocal {

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public NodoPerifericoDto getById(Long id) {
        NodoPeriferico nodo = entityManager.find(NodoPeriferico.class, id);
        return nodo == null ? null : Serializers.toDto(nodo);
    }

    @Override
    public NodoPerifericoDto getByIdentificador(String identificador) {
        List<NodoPeriferico> nodos = entityManager.createNamedQuery(NodoPeriferico.POR_IDENTIFICADOR, NodoPeriferico.class)
                .setParameter("identificador", identificador)
                .getResultList();
        return nodos.isEmpty() ? null : Serializers.toDto(nodos.get(0));
    }

    @Override
    public List<NodoPerifericoDto> getVigentes() {
        List<NodoPerifericoDto> res = new ArrayList<>();
        for (NodoPeriferico n : entityManager.createNamedQuery(NodoPeriferico.VIGENTES, NodoPeriferico.class)
                .setParameter("baja", EstadoNodo.BAJA)
                .getResultList()) {
            res.add(Serializers.toDto(n));
        }
        return res;
    }

    @Override
    public List<NodoPerifericoDto> getHabilitados(TipoNodo tipo) {
        List<NodoPerifericoDto> res = new ArrayList<>();
        for (NodoPeriferico n : entityManager.createNamedQuery(NodoPeriferico.HABILITADOS_POR_TIPO, NodoPeriferico.class)
                .setParameter("tipo", tipo)
                .setParameter("habilitado", EstadoNodo.HABILITADO)
                .getResultList()) {
            res.add(Serializers.toDto(n));
        }
        return res;
    }

    @Override
    public String getClaveCifrada(Long id) {
        NodoPeriferico nodo = entityManager.find(NodoPeriferico.class, id);
        return nodo == null ? null : nodo.getClaveCifrada();
    }

    @Override
    public void addNodo(NodoPerifericoDto nodoDto, String claveCifrada) {
        NodoPeriferico nodo = Serializers.toEntity(nodoDto);
        nodo.setClaveCifrada(claveCifrada);
        entityManager.persist(nodo);
    }

    @Override
    public void cambiarClave(Long id, String claveCifrada) {
        NodoPeriferico nodo = entityManager.find(NodoPeriferico.class, id);
        if (nodo != null) {
            nodo.setClaveCifrada(claveCifrada);
        }
    }

    @Override
    public void cambiarEstado(Long id, EstadoNodo estado) {
        NodoPeriferico nodo = entityManager.find(NodoPeriferico.class, id);
        if (nodo != null) {
            nodo.setEstado(estado);
        }
    }

    @Override
    public void cambiarPuntoAcceso(Long id, String puntoAcceso) {
        NodoPeriferico nodo = entityManager.find(NodoPeriferico.class, id);
        if (nodo != null) {
            nodo.setPuntoAcceso(puntoAcceso);
        }
    }
}
