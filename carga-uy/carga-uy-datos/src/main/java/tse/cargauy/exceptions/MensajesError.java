package tse.cargauy.exceptions;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class MensajesError {

    private static final String BUNDLE = "mensajes-error";

    private MensajesError() {
    }

    public static String resolver(CargaUYException excepcion) {
        return resolver(excepcion.getCodigo(), excepcion.getParametros());
    }

    public static String resolver(CodigoError codigo, String... parametros) {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE);
        if (!bundle.containsKey(codigo.name())) {
            return codigo.name();
        }
        return MessageFormat.format(bundle.getString(codigo.name()), (Object[]) parametros);
    }
}
