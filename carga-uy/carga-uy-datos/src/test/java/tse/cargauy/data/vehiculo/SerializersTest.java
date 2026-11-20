package tse.cargauy.data.vehiculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.Empresa;
import tse.cargauy.entities.Vehiculo;

class SerializersTest {

    @Test
    void elDtoLlevaLosDatosTecnicosYLaEmpresa() {
        Empresa empresa = new Empresa(1001, "Transportes del Sur", "Transportes del Sur S.A.", "Av. Italia 3456",
                LocalDate.of(2020, 3, 15));
        empresa.setId(1L);
        Vehiculo vehiculo = new Vehiculo("STA1234", "Volvo", "FH 460", 8500, 24000, empresa);
        vehiculo.setId(7L);

        VehiculoDto dto = Serializers.toDto(vehiculo);

        assertEquals(7L, dto.getId());
        assertEquals("STA1234", dto.getMatricula());
        assertEquals("Volvo", dto.getMarca());
        assertEquals("FH 460", dto.getModelo());
        assertEquals(8500, dto.getPesoVehiculo());
        assertEquals(24000, dto.getCapacidadCarga());
        assertEquals(1L, dto.getIdEmpresa());
        assertEquals("Transportes del Sur", dto.getNombreEmpresa());
    }

    // el id lo genera la base al persistir, aunque el dto traiga uno
    @Test
    void laEntidadNuevaUsaLaEmpresaRecibidaYNoCopiaElId() {
        Empresa empresa = new Empresa();
        VehiculoDto dto = new VehiculoDto("SBB5678", "Scania", "R 450", 9000, 26000, 1L);
        dto.setId(5L);

        Vehiculo vehiculo = Serializers.toEntity(dto, empresa);

        assertNull(vehiculo.getId());
        assertSame(empresa, vehiculo.getEmpresa());
        assertEquals("SBB5678", vehiculo.getMatricula());
        assertEquals("Scania", vehiculo.getMarca());
        assertEquals("R 450", vehiculo.getModelo());
        assertEquals(9000, vehiculo.getPesoVehiculo());
        assertEquals(26000, vehiculo.getCapacidadCarga());
    }
}
