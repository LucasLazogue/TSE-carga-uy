package tse.cargauy.data.guia;

import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.FiltroGuias;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;

@Local
public interface GuiaDAOLocal {
    GuiaDto getGuiaById(Long id);
    List<GuiaDto> getAll();
    PaginaDto<GuiaDto> buscar(FiltroGuias filtro, Paginacion paginacion);
    GuiaDto addGuia(GuiaDto guiaDto);
    void updateGuia(Long id, GuiaDto guiaDto);
    void deleteGuia(Long id);
    List<RubroDto> getRubros();
    RubroDto getRubroById(Long id);
    void addRubro(String nombre);
    List<TipoCargaDto> getTiposCarga();
    TipoCargaDto getTipoCargaById(Long id);
    void addTipoCarga(String nombre);
}
