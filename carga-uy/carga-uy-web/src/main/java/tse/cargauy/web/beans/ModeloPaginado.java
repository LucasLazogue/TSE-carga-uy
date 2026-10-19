package tse.cargauy.web.beans;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;

public class ModeloPaginado<T> extends LazyDataModel<T> {

    private static final long serialVersionUID = 1L;

    private final Paginador<T> paginador;

    public ModeloPaginado(Paginador<T> paginador) {
        this.paginador = paginador;
    }

    @Override
    public int count(Map<String, FilterMeta> filtros) {
        return (int) paginador.getPagina(new Paginacion(0, 1)).getTotal();
    }

    @Override
    public List<T> load(int desde, int tamanio, Map<String, SortMeta> orden, Map<String, FilterMeta> filtros) {
        return paginador.getPagina(new Paginacion(desde / tamanio, tamanio)).getItems();
    }

    @FunctionalInterface
    public interface Paginador<T> extends Serializable {
        PaginaDto<T> getPagina(Paginacion paginacion);
    }
}
