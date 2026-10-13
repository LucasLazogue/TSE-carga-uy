package tse.cargauy.data.fiscalizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.entities.CasoFiscalizacion;
import tse.cargauy.entities.Ciudadano;
import tse.cargauy.entities.Descargo;
import tse.cargauy.entities.Empresa;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.Evidencia;
import tse.cargauy.entities.Funcionario;
import tse.cargauy.entities.Resolucion;
import tse.cargauy.entities.TipoEvidencia;
import tse.cargauy.entities.TipoInfraccion;
import tse.cargauy.entities.Vehiculo;

class SerializersTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 13, 10, 0);

    private static CasoFiscalizacion caso() {
        Empresa empresa = new Empresa(1, "LogiEste", "LogiEste SA", "Ruta 9 km 120", LocalDate.of(2020, 1, 1));
        empresa.setId(10L);
        Vehiculo vehiculo = new Vehiculo("SBA1234", "Volvo", "FH", 15000, 30000, empresa);
        vehiculo.setId(3L);
        CasoFiscalizacion caso = new CasoFiscalizacion(TipoInfraccion.EXCESO_CARGA, "h1", AHORA.minusHours(2), AHORA, vehiculo, null);
        caso.setId(7L);
        caso.getEvidencias().add(new Evidencia(caso, TipoEvidencia.PESAJE, "b1/1", AHORA.minusHours(2), "36500 kg", "33800 kg",
                null, null));
        return caso;
    }

    @Test
    void enElListadoElCasoVaSinEvidencias() {
        CasoFiscalizacionDto dto = Serializers.toDto(caso(), false);

        assertEquals(7L, dto.getId());
        assertEquals(EstadoCaso.DETECTADA, dto.getEstado());
        assertEquals("SBA1234", dto.getMatricula());
        assertEquals(10L, dto.getIdEmpresa());
        assertEquals("LogiEste", dto.getNombreEmpresa());
        assertNull(dto.getIdViaje());
        assertNull(dto.getDescargo());
        assertTrue(dto.getEvidencias().isEmpty());
    }

    @Test
    void elDetalleIncluyeEvidenciasDescargoYResolucion() {
        CasoFiscalizacion caso = caso();
        caso.setDescargo(new Descargo(caso, "la balanza estaba descalibrada", AHORA, new Ciudadano("22222222", null)));
        caso.setResolucion(new Resolucion(caso, EstadoCaso.DESESTIMADA, "ticket valido", AHORA.plusDays(1), new Funcionario("11111111")));

        CasoFiscalizacionDto dto = Serializers.toDto(caso, true);

        assertEquals("36500 kg", dto.getEvidencias().get(0).getValorObservado());
        assertEquals("22222222", dto.getDescargo().getCedulaPresentadoPor());
        assertEquals(EstadoCaso.DESESTIMADA, dto.getResolucion().getResultado());
        assertEquals("11111111", dto.getResolucion().getCedulaFuncionario());
    }
}
