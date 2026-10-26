package tse.cargauy.data.nodo;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.TipoNodo;

@Local
public interface NodoPerifericoDAOLocal {
    NodoPerifericoDto getById(Long id);
    NodoPerifericoDto getByIdentificador(String identificador);
    List<NodoPerifericoDto> getVigentes();
    List<NodoPerifericoDto> getHabilitados(TipoNodo tipo);
    String getClaveCifrada(Long id);
    void addNodo(NodoPerifericoDto nodoDto, String claveCifrada);
    void cambiarClave(Long id, String claveCifrada);
    void cambiarEstado(Long id, EstadoNodo estado);
    void cambiarPuntoAcceso(Long id, String puntoAcceso);
}
