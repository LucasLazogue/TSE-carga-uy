package uy.tse.cargauy.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenciasRepositoryTest {

    @Test
    fun normalizarUrl_agregaLaBarraFinal() {
        assertEquals("http://10.0.2.2:8080/carga-uy/api/", PreferenciasRepository.normalizarUrl("http://10.0.2.2:8080/carga-uy/api"))
    }

    @Test
    fun normalizarUrl_noDuplicaLaBarra() {
        assertEquals("http://10.0.2.2:8080/carga-uy/api/", PreferenciasRepository.normalizarUrl("http://10.0.2.2:8080/carga-uy/api/"))
    }

    @Test
    fun normalizarUrl_sacaEspacios() {
        assertEquals("http://10.0.2.2:8080/carga-uy/api/", PreferenciasRepository.normalizarUrl("  http://10.0.2.2:8080/carga-uy/api/ "))
    }
}
