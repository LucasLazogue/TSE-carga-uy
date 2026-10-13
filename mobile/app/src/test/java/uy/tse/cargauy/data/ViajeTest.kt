package uy.tse.cargauy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

import uy.tse.cargauy.data.local.EventoPendiente
import uy.tse.cargauy.data.remote.GuiaDto
import uy.tse.cargauy.data.remote.ViajeDto
import uy.tse.cargauy.data.remote.ViajeMovilDto

class ViajeTest {

    // las mismas transiciones que AplicadorEventosEJB.transicion en el backend
    @Test
    fun estadoTras_sigueLaMaquinaDeEstadosDelBackend() {
        assertEquals("EN_CURSO", estadoTras("ASIGNADO", TipoEvento.INICIO))
        assertEquals("EN_CURSO", estadoTras("EN_CURSO", TipoEvento.CARGA_PARCIAL))
        assertEquals("EN_CURSO", estadoTras("EN_CURSO", TipoEvento.INCIDENTE))
        assertEquals("FINALIZADO", estadoTras("EN_CURSO", TipoEvento.FIN))
    }

    @Test
    fun estadoTras_eventoQueNoCorrespondeNoCambiaNada() {
        assertNull(estadoTras("ASIGNADO", TipoEvento.FIN))
        assertNull(estadoTras("ASIGNADO", TipoEvento.CARGA_PARCIAL))
        assertNull(estadoTras("EN_CURSO", TipoEvento.INICIO))
        assertNull(estadoTras("FINALIZADO", TipoEvento.DESCARGA_PARCIAL))
    }

    @Test
    fun aEventoDto_mandaLaHoraEnIsoUtc() {
        val evento = EventoPendiente("e1", idUsuario = 2, idViaje = 2, tipo = "INICIO", timestampGeneracion = 1_791_727_402_123)
        val dto = aEventoDto(evento)
        assertEquals("2026-10-11T14:03:22.123Z", dto.timestampGeneracion)
        assertEquals(2L, dto.idViaje)
        assertEquals("INICIO", dto.tipo)
    }

    @Test
    fun aViajeLocal_completaLoQueFaltaConValoresPorDefecto() {
        val dto = ViajeMovilDto(
            viaje = ViajeDto(id = 2, estado = "ASIGNADO", matricula = "STA1234"),
            guia = GuiaDto(id = 52, nroGuia = "G-000052", fecha = "2026-10-11", volumen = 12000)
        )
        val local = aViajeLocal(dto, idUsuario = 7)
        assertEquals(2L, local.idViaje)
        assertEquals(7L, local.idUsuario)
        assertEquals("G-000052", local.nroGuia)
        assertEquals("STA1234", local.matricula)
        assertEquals("", local.rubro)
        assertEquals(0.0, local.origenLat, 0.0)
    }
}
