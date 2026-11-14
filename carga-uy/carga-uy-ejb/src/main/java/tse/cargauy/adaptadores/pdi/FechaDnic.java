package tse.cargauy.adaptadores.pdi;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// DNIC informa la fecha de nacimiento como "yyyy-mmdd", y puede venir vacia o solo con anio-mes o anio
public final class FechaDnic {

    private static final int MAYORIA_DE_EDAD = 18;
    private static final Pattern FORMATO = Pattern.compile("(\\d{4})(?:-(\\d{2})(?:-?(\\d{2}))?)?");

    private FechaDnic() {
    }

    // devuelve yyyy-MM-dd, yyyy-MM o yyyy; null si esta vacia o no es una fecha
    public static String normalizar(String fecha) {
        if (fecha == null) {
            return null;
        }
        Matcher partes = FORMATO.matcher(fecha.trim());
        if (!partes.matches()) {
            return null;
        }
        String normalizada = partes.group(1);
        if (partes.group(2) != null) {
            normalizada += "-" + partes.group(2);
        }
        if (partes.group(3) != null) {
            normalizada += "-" + partes.group(3);
        }
        try {
            ultimoDiaPosible(normalizada);
            return normalizada;
        } catch (DateTimeException e) {
            return null;
        }
    }

    public static boolean esMenor(String fechaNormalizada, LocalDate hoy) {
        return fechaNormalizada != null
                && ultimoDiaPosible(fechaNormalizada).plusYears(MAYORIA_DE_EDAD).isAfter(hoy);
    }

    private static LocalDate ultimoDiaPosible(String fecha) {
        return switch (fecha.length()) {
            case 4 -> Year.parse(fecha).atMonth(12).atEndOfMonth();
            case 7 -> YearMonth.parse(fecha).atEndOfMonth();
            default -> LocalDate.parse(fecha);
        };
    }
}
