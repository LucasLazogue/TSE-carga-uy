package tse.cargauy.negocio.nodo;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.NodoPerifericoDto;

@Local
public interface NodoPerifericoEJBLocal {
    List<NodoPerifericoDto> getVigentes();
    NodoPerifericoDto getById(Long id);
    void addNodo(NodoPerifericoDto nodoDto);
    void habilitar(Long id);
    void deshabilitar(Long id);
    void cambiarPuntoAcceso(Long id, String puntoAcceso);
    void darDeBaja(Long id);
}
