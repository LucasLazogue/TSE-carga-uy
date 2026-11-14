package tse.cargauy.pdisuplantador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tse.cargauy.pdisuplantador.soap.ObjPersona;
import tse.cargauy.pdisuplantador.soap.ObtProductInfo;
import tse.cargauy.pdisuplantador.soap.ParamObtPersonaPorDoc;
import tse.cargauy.pdisuplantador.soap.ParamProductDesc;
import tse.cargauy.pdisuplantador.soap.ResultObtPersonaPorDoc;

class ServicioBasicoInformacionImplTest {

    private final Map<String, String> variables = new HashMap<>(Map.of(
            "PDI_ORGANIZACION", "carga-uy",
            "PDI_PASSWORD_ENTIDAD", "clave"));

    private final ServicioBasicoInformacionImpl servicio = new ServicioBasicoInformacionImpl() {
        @Override
        String entorno(String variable) {
            return variables.get(variable);
        }
    };

    private static ParamObtPersonaPorDoc consulta(String cedula) {
        ParamObtPersonaPorDoc parametros = new ParamObtPersonaPorDoc();
        parametros.setOrganizacion("carga-uy");
        parametros.setPasswordEntidad("clave");
        parametros.setNroDocumento(cedula);
        parametros.setTipoDocumento("DO");
        return parametros;
    }

    private static void assertError(CodigoDnic esperado, ResultObtPersonaPorDoc resultado) {
        assertNull(resultado.getObjPersona());
        assertEquals(esperado.getCodigo(), resultado.getErrores().getMensajes().get(0).getCodMensaje());
        assertEquals(esperado.getDescripcion(), resultado.getErrores().getMensajes().get(0).getDescripcion());
        assertNotNull(resultado.getErrores().getMensajes().get(0).getDatoExtra());
    }

    private void conArchivo(Path carpeta, String personas) throws IOException {
        Path archivo = Files.writeString(carpeta.resolve("personas.json"), personas);
        variables.put("PDI_PERSONAS_ARCHIVO", archivo.toString());
    }

    private static String fechaDelMenor() {
        return LocalDate.now().minusYears(16).format(DateTimeFormatter.ofPattern("yyyy-MMdd"));
    }

    @Test
    void devuelveLosDatosFiliatoriosDelArchivo(@TempDir Path carpeta) throws IOException {
        conArchivo(carpeta, """
                [{"cedula": "22222222", "nombre1": "Ana", "nombre2": "María", "apellido1": "Pereira",
                  "apellido2": "Gómez", "sexo": 2, "fechaNacimiento": "1975-1103"}]""");

        ResultObtPersonaPorDoc resultado = servicio.obtPersonaPorDoc(consulta("22222222"));

        ObjPersona persona = resultado.getObjPersona();
        assertEquals("DO", persona.getCodTipoDocumento());
        assertEquals("22222222", persona.getNroDocumento());
        assertEquals("Ana", persona.getNombre1());
        assertEquals("María", persona.getNombre2());
        assertEquals("Pereira", persona.getApellido1());
        assertEquals("Gómez", persona.getApellido2());
        assertEquals(2, persona.getSexo());
        assertEquals("1975-1103", persona.getFechaNacimiento());
        assertEquals(1, persona.getCodNacionalidad());
        assertNull(resultado.getErrores());
        assertNull(resultado.getWarnings());
    }

    @Test
    void elMenorSigueSiendoMenorSinImportarElDia() {
        assertEquals(fechaDelMenor(),
                servicio.obtPersonaPorDoc(consulta("33333333")).getObjPersona().getFechaNacimiento());
    }

    @Test
    void losDatosARegularizarVuelvenConAdvertencia() {
        ResultObtPersonaPorDoc resultado = servicio.obtPersonaPorDoc(consulta("66666666"));

        assertNotNull(resultado.getObjPersona());
        assertEquals(701, resultado.getWarnings().getMensajes().get(0).getCodMensaje());
        assertNull(resultado.getErrores());
    }

    @Test
    void cedulaAnulada() {
        assertError(CodigoDnic.CEDULA_ANULADA, servicio.obtPersonaPorDoc(consulta("77777777")));
    }

    @Test
    void falloTransitorio() {
        assertError(CodigoDnic.CONSULTA_INCOMPLETA, servicio.obtPersonaPorDoc(consulta("88888888")));
    }

    @Test
    void personaInexistente() {
        assertError(CodigoDnic.PERSONA_INEXISTENTE, servicio.obtPersonaPorDoc(consulta("12345678")));
    }

    @Test
    void losCasosEspecialesGananSobreElArchivo(@TempDir Path carpeta) throws IOException {
        conArchivo(carpeta, """
                [{"cedula": "33333333", "nombre1": "Lucía", "apellido1": "Fernández", "fechaNacimiento": "1990"},
                 {"cedula": "77777777", "nombre1": "Pedro", "apellido1": "Gómez"}]""");

        assertEquals(fechaDelMenor(),
                servicio.obtPersonaPorDoc(consulta("33333333")).getObjPersona().getFechaNacimiento());
        assertError(CodigoDnic.CEDULA_ANULADA, servicio.obtPersonaPorDoc(consulta("77777777")));
    }

    @Test
    void cedulaConSignosSonParametrosIncorrectos() {
        assertError(CodigoDnic.PARAMETROS_INCORRECTOS, servicio.obtPersonaPorDoc(consulta("5.555.555-5")));
    }

    @Test
    void otroTipoDeDocumentoSonParametrosIncorrectos() {
        ParamObtPersonaPorDoc parametros = consulta("55555555");
        parametros.setTipoDocumento("PA");

        assertError(CodigoDnic.PARAMETROS_INCORRECTOS, servicio.obtPersonaPorDoc(parametros));
    }

    @Test
    void passwordEquivocadaNoEstaAutorizada() {
        ParamObtPersonaPorDoc parametros = consulta("55555555");
        parametros.setPasswordEntidad("otra");

        assertError(CodigoDnic.ACCESO_NO_AUTORIZADO, servicio.obtPersonaPorDoc(parametros));
    }

    @Test
    void otraOrganizacionNoEstaAutorizada() {
        ParamObtPersonaPorDoc parametros = consulta("55555555");
        parametros.setOrganizacion("otra");

        assertError(CodigoDnic.ACCESO_NO_AUTORIZADO, servicio.obtPersonaPorDoc(parametros));
    }

    @Test
    void sinPasswordConfiguradaNadieEstaAutorizado() {
        variables.remove("PDI_PASSWORD_ENTIDAD");

        assertError(CodigoDnic.ACCESO_NO_AUTORIZADO, servicio.obtPersonaPorDoc(consulta("55555555")));
    }

    @Test
    void conLaFallaActivadaTodoEsFalloTransitorio() {
        variables.put("PDI_FALLA", "true");

        assertError(CodigoDnic.CONSULTA_INCOMPLETA, servicio.obtPersonaPorDoc(consulta("55555555")));
    }

    @Test
    void laDemoraRetrasaLaRespuesta() {
        variables.put("PDI_DEMORA_MS", "200");
        long inicio = System.nanoTime();

        servicio.obtPersonaPorDoc(consulta("55555555"));

        assertTrue(System.nanoTime() - inicio >= 200_000_000L);
    }

    @Test
    void productDescInformaVersionYModalidad() {
        ObtProductInfo info = servicio.productDesc(new ParamProductDesc()).getObtProductInfo();

        assertEquals("1.0", info.getVersion());
        assertEquals("Testing", info.getModalidad());
        assertEquals("Servicio de Informacion D.N.I.C.", info.getDescripcion());
    }
}
