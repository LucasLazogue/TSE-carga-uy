package tse.cargauy.data.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.Ciudadano;

class SerializersTest {

    // el ingreso usa este DTO para saber si el perfil esta pendiente (sin nombres) y si es menor (por la fecha)
    @Test
    void elCiudadanoConservaLosDatosDeLaPdi() {
        Ciudadano ciudadano = new Ciudadano("22222222", "ana@correo.uy");
        ciudadano.setNombres("Ana María");
        ciudadano.setApellidos("Pereira Gómez");
        ciudadano.setFechaNacimiento("1975-11-03");

        UsuarioDto leido = Serializers.toDto(ciudadano);

        assertEquals("22222222", leido.getCedula());
        assertEquals("ana@correo.uy", leido.getCorreo());
        assertEquals("Ana María", leido.getNombres());
        assertEquals("Pereira Gómez", leido.getApellidos());
        assertEquals("1975-11-03", leido.getFechaNacimiento());
    }
}
