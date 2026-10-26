package tse.cargauy.dtos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PesajesViajeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<PesajeDto> pesajes = new ArrayList<>();
    private List<String> nodosSinResponder = new ArrayList<>();

    public PesajesViajeDto() {
    }

    // si algun nodo no respondio no se puede afirmar que no hubo exceso: la evaluacion queda pendiente
    public boolean isCompleto() {
        return nodosSinResponder.isEmpty();
    }

    public List<PesajeDto> getPesajes() {
        return pesajes;
    }

    public void setPesajes(List<PesajeDto> pesajes) {
        this.pesajes = pesajes;
    }

    public List<String> getNodosSinResponder() {
        return nodosSinResponder;
    }

    public void setNodosSinResponder(List<String> nodosSinResponder) {
        this.nodosSinResponder = nodosSinResponder;
    }
}
