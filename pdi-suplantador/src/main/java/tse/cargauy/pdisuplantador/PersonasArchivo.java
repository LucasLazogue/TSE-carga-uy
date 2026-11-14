package tse.cargauy.pdisuplantador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.JsonException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import tse.cargauy.pdisuplantador.soap.ObjPersona;

// todas las personas salvo los casos especiales. se lee en cada consulta para que un cambio se vea sin reiniciar: el
// archivo es chico y las consultas son las de los ingresos de una demo
final class PersonasArchivo {

    private static final Logger LOG = Logger.getLogger(PersonasArchivo.class.getName());
    private static final Jsonb JSON = JsonbBuilder.create();
    private static final String CEDULA = "\\d{6,8}";
    private static final String FECHA_DNIC = "\\d{4}(-(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])?)?";

    private PersonasArchivo() {
    }

    // publico porque JSON-B lo lee desde afuera del paquete
    public record Persona(String cedula, String nombre1, String nombre2, String apellido1, String apellido2,
            Integer sexo, String fechaNacimiento) {
    }

    // null si la cedula no esta o si el archivo no se puede usar: solo quedan los casos especiales
    static ObjPersona buscar(String ruta, String cedula) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        for (Persona persona : leer(ruta)) {
            if (persona.cedula().equals(cedula)) {
                return ServicioBasicoInformacionImpl.persona(persona.cedula(), persona.nombre1(), persona.nombre2(),
                        persona.apellido1(), persona.apellido2(), persona.sexo(), persona.fechaNacimiento());
            }
        }
        return null;
    }

    private static Persona[] leer(String ruta) {
        Persona[] personas;
        try {
            personas = JSON.fromJson(Files.readString(Path.of(ruta)), Persona[].class);
        } catch (NoSuchFileException e) {
            LOG.warning("No se encontro el archivo de personas: solo responden los casos especiales.");
            return new Persona[0];
        } catch (IOException | JsonException | JsonbException e) {
            // sin el detalle de la excepcion: puede citar el contenido del archivo
            LOG.severe("No se pudo leer el archivo de personas: se ignora. Tiene que ser una lista JSON con los campos "
                    + "del README.");
            return new Persona[0];
        }
        return todasValidas(personas) ? personas : new Persona[0];
    }

    // un error descarta todo el archivo: asi nunca queda cargado a medias sin que se note
    private static boolean todasValidas(Persona[] personas) {
        if (personas == null) {
            LOG.severe("El archivo de personas esta vacio: se ignora.");
            return false;
        }
        for (int i = 0; i < personas.length; i++) {
            if (!valida(personas[i])) {
                LOG.log(Level.SEVERE, "La persona numero {0} del archivo no es valida: se ignora el archivo. Hacen "
                        + "falta cedula de 6 a 8 digitos, nombre1 y apellido1; la fecha es yyyy-mmdd, yyyy-mm o yyyy.",
                        i + 1);
                return false;
            }
        }
        return true;
    }

    private static boolean valida(Persona persona) {
        return persona != null
                && persona.cedula() != null && persona.cedula().matches(CEDULA)
                && persona.nombre1() != null && !persona.nombre1().isBlank()
                && persona.apellido1() != null && !persona.apellido1().isBlank()
                && (persona.fechaNacimiento() == null || persona.fechaNacimiento().matches(FECHA_DNIC));
    }
}
