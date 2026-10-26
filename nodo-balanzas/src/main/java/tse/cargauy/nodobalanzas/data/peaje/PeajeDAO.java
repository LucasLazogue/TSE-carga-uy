package tse.cargauy.nodobalanzas.data.peaje;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.nodobalanzas.dtos.BalanzaDto;
import tse.cargauy.nodobalanzas.dtos.PeajeDto;
import tse.cargauy.nodobalanzas.entities.Balanza;
import tse.cargauy.nodobalanzas.entities.Peaje;

@Stateless
public class PeajeDAO implements PeajeDAOLocal {

    @PersistenceContext(unitName = "nodo-balanzas")
    private EntityManager entityManager;

    @Override
    public List<PeajeDto> getPeajes() {
        List<PeajeDto> res = new ArrayList<>();
        for (Peaje p : entityManager.createNamedQuery(Peaje.TODOS, Peaje.class).getResultList()) {
            res.add(Serializers.toDtoConBalanzas(p));
        }
        return res;
    }

    @Override
    public PeajeDto getPeajeByCodigo(String codigo) {
        Peaje peaje = peaje(codigo);
        return peaje == null ? null : Serializers.toDto(peaje);
    }

    @Override
    public void addPeaje(PeajeDto peajeDto) {
        entityManager.persist(Serializers.toEntity(peajeDto));
    }

    @Override
    public BalanzaDto getBalanzaByCodigo(String codigo) {
        List<Balanza> balanzas = entityManager.createNamedQuery(Balanza.POR_CODIGO, Balanza.class)
                .setParameter("codigo", codigo)
                .getResultList();
        return balanzas.isEmpty() ? null : Serializers.toDto(balanzas.get(0));
    }

    @Override
    public String getCodigoBalanzaByClave(String claveHash) {
        List<Balanza> balanzas = entityManager.createNamedQuery(Balanza.POR_CLAVE, Balanza.class)
                .setParameter("claveHash", claveHash)
                .getResultList();
        return balanzas.isEmpty() ? null : balanzas.get(0).getCodigo();
    }

    @Override
    public void addBalanza(String codigoPeaje, BalanzaDto balanzaDto, String claveHash) {
        Balanza balanza = new Balanza();
        balanza.setPeaje(peaje(codigoPeaje));
        balanza.setCodigo(balanzaDto.getCodigo());
        balanza.setSentido(balanzaDto.getSentido());
        balanza.setClaveHash(claveHash);
        entityManager.persist(balanza);
    }

    private Peaje peaje(String codigo) {
        List<Peaje> peajes = entityManager.createNamedQuery(Peaje.POR_CODIGO, Peaje.class)
                .setParameter("codigo", codigo)
                .getResultList();
        return peajes.isEmpty() ? null : peajes.get(0);
    }
}
