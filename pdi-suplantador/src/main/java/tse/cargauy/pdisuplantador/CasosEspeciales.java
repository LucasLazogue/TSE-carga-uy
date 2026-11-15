package tse.cargauy.pdisuplantador;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import tse.cargauy.pdisuplantador.soap.ObjPersona;

// cedulas que simulan comportamientos del servicio, que no se pueden escribir como datos fijos en personas.json.
// tienen prioridad sobre el archivo
final class CasosEspeciales {

    private static final DateTimeFormatter FORMATO_DNIC = DateTimeFormatter.ofPattern("yyyy-MMdd");
    private static final int EDAD_DEL_MENOR = 16;
    private static final int FEMENINO = 2;
    private static final String MENOR = "33333333";
    private static final String A_REGULARIZAR = "66666666";

    private static final Map<String, CodigoDnic> ERRORES = Map.of(
            "77777777", CodigoDnic.CEDULA_ANULADA,
            "88888888", CodigoDnic.CONSULTA_INCOMPLETA);

    private CasosEspeciales() {
    }

    static CodigoDnic error(String cedula) {
        return ERRORES.get(cedula);
    }

    static boolean aRegularizar(String cedula) {
        return A_REGULARIZAR.equals(cedula);
    }

    // null si la cedula no es un caso especial con datos
    static ObjPersona buscar(String cedula, LocalDate hoy) {
        return switch (cedula) {
            // se calcula desde hoy para que siga siendo menor sin importar cuando se corra la demo
            case MENOR -> ServicioBasicoInformacionImpl.persona(cedula, "Lucía", null, "Fernández", null, FEMENINO,
                    hoy.minusYears(EDAD_DEL_MENOR).format(FORMATO_DNIC));
            case A_REGULARIZAR -> ServicioBasicoInformacionImpl.persona(cedula, "Marta", null, "Núñez", null, FEMENINO,
                    "1990-0527");
            default -> null;
        };
    }
}
