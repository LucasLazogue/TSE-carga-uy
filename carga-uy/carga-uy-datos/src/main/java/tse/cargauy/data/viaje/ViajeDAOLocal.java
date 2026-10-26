package tse.cargauy.data.viaje;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;

@Local
public interface ViajeDAOLocal {
    ViajeDto getViajeById(Long id);
    ViajeDto getByGuia(Long idGuia);
    PaginaDto<ViajeDto> getAll(FiltroViajes filtro, Paginacion paginacion);
    ViajeDto addViaje(ViajeDto viajeDto);
    void updateViaje(Long id, ViajeDto viajeDto);
    void deleteViaje(Long id);
    // los viajes asignados y en curso del chofer, primero el que esta en curso
    List<ViajeDto> getActivosDelChofer(Long idCiudadano);
    // con la fila bloqueada hasta el fin de la transaccion
    ViajeDto getViajeParaActualizar(Long id);
    void cambiarEstado(Long id, EstadoViaje estado, LocalDateTime fecha);
    void corregirInicio(Long id, LocalDateTime fecha);
    // si otro viaje del chofer (o del vehiculo) estuvo en curso en algun momento de [desde, hasta); hasta null = sin fin
    boolean choferConOtroEnCurso(Long idViaje, Long idCiudadano, LocalDateTime desde, LocalDateTime hasta);
    boolean vehiculoConOtroEnCurso(Long idViaje, Long idVehiculo, LocalDateTime desde, LocalDateTime hasta);
    // el viaje que el vehiculo tenia en curso en ese momento, o null
    ViajeDto getEnCursoDelVehiculo(Long idVehiculo, LocalDateTime momento);
    // los viajes del vehiculo que empezaron y seguian en curso en 'desde' o empezaron despues, por fecha de inicio
    List<ViajeDto> getIniciadosDelVehiculo(Long idVehiculo, LocalDateTime desde);
}
