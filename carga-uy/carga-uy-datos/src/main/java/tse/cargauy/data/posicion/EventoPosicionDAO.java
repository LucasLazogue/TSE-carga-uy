package tse.cargauy.data.posicion;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import tse.cargauy.dtos.EventoPosicionDto;

@Stateless
public class EventoPosicionDAO implements EventoPosicionDAOLocal {

    @EJB
    AlmacenPosiciones almacen;

    // MongoDB no participa de la transaccion del consumidor: si despues falla otro paso y el broker reentrega el
    // mensaje, la posicion ya esta guardada y el indice unico la rechaza. Por eso un duplicado no es un error (DA07)
    @Override
    public boolean registrar(EventoPosicionDto eventoDto) {
        Document documento = Serializers.toDocument(eventoDto).append("procesada", false);
        try {
            almacen.posiciones().insertOne(documento);
            return true;
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public EventoPosicionDto get(Long idNodo, String idEvento) {
        Document d = almacen.posiciones().find(origen(idNodo, idEvento)).first();
        return d == null ? null : Serializers.toDto(d);
    }

    // se llama cuando la transaccion del consumidor ya se confirmo, asi que no hay transaccion a la que unirse
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void marcarProcesada(Long idNodo, String idEvento) {
        almacen.posiciones().updateOne(origen(idNodo, idEvento), Updates.set("procesada", true));
    }

    @Override
    public List<EventoPosicionDto> getRuta(String matricula) {
        List<EventoPosicionDto> res = new ArrayList<>();
        for (Document d : almacen.posiciones()
                .find(Filters.eq("matricula", matricula))
                .sort(Sorts.ascending("timestampGeneracion"))) {
            res.add(Serializers.toDto(d));
        }
        return res;
    }

    // los mismos campos del indice unico
    private static Bson origen(Long idNodo, String idEvento) {
        return Filters.and(Filters.eq("idNodo", idNodo), Filters.eq("idEvento", idEvento));
    }
}
