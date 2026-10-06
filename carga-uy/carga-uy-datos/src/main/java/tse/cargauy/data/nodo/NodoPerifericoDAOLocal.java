package tse.cargauy.data.nodo;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.entities.EstadoNodo;

@Local
public interface NodoPerifericoDAOLocal {
    NodoPerifericoDto getById(Long id);
    NodoPerifericoDto getByIdentificador(String identificador);
    List<NodoPerifericoDto> getVigentes();
    void addNodo(NodoPerifericoDto nodoDto);
    void cambiarEstado(Long id, EstadoNodo estado);
    void cambiarPuntoAcceso(Long id, String puntoAcceso);
}
