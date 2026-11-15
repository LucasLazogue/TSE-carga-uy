package tse.cargauy.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class UsuarioActualDtoTest {

    @Test
    void copiaElUsuarioYAgregaSusEmpresas() {
        UsuarioDto usuario = new UsuarioDto("ana", List.of("CIUDADANO", "RESPONSABLE"));
        usuario.setId(7L);
        usuario.setCedula("22222222");
        usuario.setCorreo("ana@correo.uy");
        usuario.setNombres("Ana María");
        usuario.setApellidos("Pereira Gómez");
        usuario.setFechaNacimiento("1975-11-03");
        List<EmpresaDto> empresas = List.of(new EmpresaDto());

        UsuarioActualDto actual = new UsuarioActualDto(usuario, empresas);

        assertEquals(7L, actual.getId());
        assertEquals("ana", actual.getUsuario());
        assertEquals("22222222", actual.getCedula());
        assertEquals("ana@correo.uy", actual.getCorreo());
        assertEquals("Ana María", actual.getNombres());
        assertEquals("Pereira Gómez", actual.getApellidos());
        assertEquals("1975-11-03", actual.getFechaNacimiento());
        assertEquals(List.of("CIUDADANO", "RESPONSABLE"), actual.getRoles());
        assertEquals(empresas, actual.getEmpresas());
    }
}
