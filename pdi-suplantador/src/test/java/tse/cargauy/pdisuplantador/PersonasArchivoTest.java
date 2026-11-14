package tse.cargauy.pdisuplantador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tse.cargauy.pdisuplantador.soap.ObjPersona;

class PersonasArchivoTest {

    private static final String PEDRO = """
            [{"cedula": "12345672", "nombre1": "Pedro", "nombre2": "Luis", "apellido1": "Gómez", "apellido2": "Díaz",
              "sexo": 1, "fechaNacimiento": "1992-0815"}]""";

    @TempDir
    Path carpeta;

    // el que usan el Compose y la imagen: con un error se ignoraria entero y los ciudadanos de DatosPrueba quedarian sin
    // datos
    @ParameterizedTest
    @ValueSource(strings = {"55555555", "22222222"})
    void elArchivoDelRepoTieneALosCiudadanosDeDatosPrueba(String cedula) {
        assertNotNull(PersonasArchivo.buscar("personas/personas.json", cedula));
    }

    private String archivo(String contenido) throws IOException {
        return Files.writeString(carpeta.resolve("personas.json"), contenido).toString();
    }

    @Test
    void devuelveLosDatosDelArchivo() throws IOException {
        ObjPersona persona = PersonasArchivo.buscar(archivo(PEDRO), "12345672");

        assertEquals("DO", persona.getCodTipoDocumento());
        assertEquals("12345672", persona.getNroDocumento());
        assertEquals("Pedro", persona.getNombre1());
        assertEquals("Luis", persona.getNombre2());
        assertEquals("Gómez", persona.getApellido1());
        assertEquals("Díaz", persona.getApellido2());
        assertEquals(1, persona.getSexo());
        assertEquals("1992-0815", persona.getFechaNacimiento());
    }

    @Test
    void soloSonObligatoriosCedulaNombreYApellido() throws IOException {
        String ruta = archivo("""
                [{"cedula": "123456", "nombre1": "Pedro", "apellido1": "Gómez"}]""");

        ObjPersona persona = PersonasArchivo.buscar(ruta, "123456");

        assertEquals("Pedro", persona.getNombre1());
        assertNull(persona.getNombre2());
        assertNull(persona.getSexo());
        assertNull(persona.getFechaNacimiento());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1992-0815", "1992-08", "1992"})
    void aceptaLasFechasDeDnic(String fecha) throws IOException {
        String ruta = archivo("""
                [{"cedula": "12345672", "nombre1": "Pedro", "apellido1": "Gómez", "fechaNacimiento": "%s"}]"""
                .formatted(fecha));

        assertEquals(fecha, PersonasArchivo.buscar(ruta, "12345672").getFechaNacimiento());
    }

    @Test
    void unaCedulaQueNoEstaNoDevuelveNada() throws IOException {
        assertNull(PersonasArchivo.buscar(archivo(PEDRO), "34567894"));
    }

    @Test
    void tomaLosCambiosSinReiniciar() throws IOException {
        String ruta = archivo("[]");
        assertNull(PersonasArchivo.buscar(ruta, "12345672"));

        archivo(PEDRO);

        assertNotNull(PersonasArchivo.buscar(ruta, "12345672"));
    }

    @Test
    void sinRutaConfiguradaNoDevuelveNada() {
        assertNull(PersonasArchivo.buscar(null, "12345672"));
        assertNull(PersonasArchivo.buscar(" ", "12345672"));
    }

    @Test
    void unArchivoQueNoExisteNoDevuelveNada() {
        assertNull(PersonasArchivo.buscar(carpeta.resolve("no-existe.json").toString(), "12345672"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "esto no es json", "{\"cedula\": \"12345672\"}", "[{\"cedula\": "})
    void unArchivoMalFormadoSeIgnora(String contenido) throws IOException {
        assertNull(PersonasArchivo.buscar(archivo(contenido), "12345672"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "null",
        "{\"cedula\": \"1.234.567-2\", \"nombre1\": \"Ana\", \"apellido1\": \"Silva\"}",
        "{\"nombre1\": \"Ana\", \"apellido1\": \"Silva\"}",
        "{\"cedula\": \"34567894\", \"apellido1\": \"Silva\"}",
        "{\"cedula\": \"34567894\", \"nombre1\": \" \", \"apellido1\": \"Silva\"}",
        "{\"cedula\": \"34567894\", \"nombre1\": \"Ana\"}",
        "{\"cedula\": \"34567894\", \"nombre1\": \"Ana\", \"apellido1\": \"Silva\", \"fechaNacimiento\": \"1992-08-15\"}",
        "{\"cedula\": \"34567894\", \"nombre1\": \"Ana\", \"apellido1\": \"Silva\", \"fechaNacimiento\": \"1992-1315\"}"
    })
    void unaPersonaInvalidaDescartaTodoElArchivo(String invalida) throws IOException {
        String ruta = archivo(PEDRO.replaceFirst("]$", ", " + invalida + "]"));

        assertNull(PersonasArchivo.buscar(ruta, "12345672"));
    }
}
