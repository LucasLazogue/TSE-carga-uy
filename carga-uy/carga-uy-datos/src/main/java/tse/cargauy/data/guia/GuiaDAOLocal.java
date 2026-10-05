package tse.cargauy.data.guia;

import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;

@Local
public interface GuiaDAOLocal {
    GuiaDto getGuiaById(Long id);
    List<GuiaDto> getAll();
    List<GuiaDto> getByEmpresa(Long idEmpresa);
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
