package tse.cargauy.negocio.informe;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public final class TextosInforme {

    private static final String BUNDLE = "informes";

    private TextosInforme() {
    }

    public static String resolver(String clave, Object... parametros) {
        return MessageFormat.format(ResourceBundle.getBundle(BUNDLE).getString(clave), parametros);
    }
}
