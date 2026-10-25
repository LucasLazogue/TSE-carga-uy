package tse.cargauy.nodobalanzas.data.pesaje;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;
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
    public List<PesajeDto> getPesajesByRut(String rut, LocalDateTime desde, LocalDateTime hasta) {
        List<PesajeDto> res = new ArrayList<>();
        for (Pesaje p : entityManager.createNamedQuery(Pesaje.POR_RUT, Pesaje.class)
                .setParameter("rut", rut)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList()) {
            res.add(Serializers.toDto(p));
        }
        return res;
    }
}
