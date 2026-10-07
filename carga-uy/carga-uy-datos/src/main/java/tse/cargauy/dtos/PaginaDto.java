package tse.cargauy.dtos;

import java.io.Serializable;
import java.util.List;

public class PaginaDto<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> items;
    private long total;
    private int pagina;
    private int tamanio;

    public PaginaDto() {
    }

    public PaginaDto(List<T> items, long total, int pagina, int tamanio) {
        this.items = items;
        this.total = total;
        this.pagina = pagina;
        this.tamanio = tamanio;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPagina() {
        return pagina;
    }

    public void setPagina(int pagina) {
        this.pagina = pagina;
    }

    public int getTamanio() {
        return tamanio;
    }

    public void setTamanio(int tamanio) {
        this.tamanio = tamanio;
    }
}
