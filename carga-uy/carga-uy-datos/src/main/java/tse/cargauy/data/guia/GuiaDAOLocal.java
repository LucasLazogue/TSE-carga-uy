package tse.cargauy.data.guia;

import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;

@Local
public interface GuiaDAOLocal {
    GuiaDto getGuiaById(Long id);
    GuiaDto getGuiaByNro(String nroGuia);
    List<GuiaDto> getAll();
    List<GuiaDto> getByEmpresa(Long idEmpresa);
    GuiaDto addGuia(GuiaDto guiaDto);
    void updateGuia(Long id, GuiaDto guiaDto);
    void deleteGuia(Long id);
    List<RubroDto> getRubros();
    void addRubro(String nombre);
    List<TipoCargaDto> getTiposCarga();
    void addTipoCarga(String nombre);
}
