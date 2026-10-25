package tse.cargauy.nodobalanzas.data.pesaje;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;
import tse.cargauy.nodobalanzas.entities.Balanza;
import tse.cargauy.nodobalanzas.entities.Pesaje;

@Stateless
public class PesajeDAO implements PesajeDAOLocal {

    @PersistenceContext(unitName = "nodo-balanzas")
    private EntityManager entityManager;

    @Override
    public List<PesajeDto> getPesajesByMatricula(String matricula, LocalDateTime desde, LocalDateTime hasta) {
        List<PesajeDto> res = new ArrayList<>();
        for (Pesaje p : entityManager.createNamedQuery(Pesaje.POR_MATRICULA, Pesaje.class)
                .setParameter("matricula", matricula)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList()) {
            res.add(Serializers.toDto(p));
        }
        return res;
    }

    @Override
    public boolean existePesaje(String codigoBalanza, String idRegistro) {
        return entityManager.createNamedQuery(Pesaje.CANTIDAD_POR_REGISTRO, Long.class)
                .setParameter("balanza", codigoBalanza)
                .setParameter("idRegistro", idRegistro)
                .getSingleResult() > 0;
    }

    @Override
    public void addPesaje(String codigoBalanza, PesajeDto pesajeDto, LocalDateTime recibidoEn) {
        Pesaje pesaje = new Pesaje();
        pesaje.setBalanza(entityManager.createNamedQuery(Balanza.POR_CODIGO, Balanza.class)
                .setParameter("codigo", codigoBalanza)
                .getSingleResult());
        pesaje.setIdRegistro(pesajeDto.getIdRegistro());
        pesaje.setMatricula(pesajeDto.getMatricula());
        pesaje.setFechaHora(pesajeDto.getFechaHora());
        pesaje.setPesoRegistrado(pesajeDto.getPesoRegistrado());
        pesaje.setRecibidoEn(recibidoEn);
        entityManager.persist(pesaje);
    }
}
