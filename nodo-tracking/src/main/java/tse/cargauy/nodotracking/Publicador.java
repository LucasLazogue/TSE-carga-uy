package tse.cargauy.nodotracking;

import java.util.List;

public interface Publicador {
    void publicar(List<String> mensajes) throws PublicadorException;
    boolean disponible();
}
