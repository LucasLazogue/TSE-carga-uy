package tse.cargauy.negocio;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

// mensajes JMS sin broker, para probar los MDB llamando a onMessage
public final class MensajesJms {

    private MensajesJms() {
    }

    public static TextMessage texto(String cuerpo) {
        return texto(cuerpo, Map.of(), 0);
    }

    public static TextMessage texto(String cuerpo, Map<String, String> propiedades, long publicado) {
        return (TextMessage) Proxy.newProxyInstance(MensajesJms.class.getClassLoader(),
                new Class<?>[] { TextMessage.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "getText" -> cuerpo;
                    case "getJMSTimestamp" -> publicado;
                    case "getPropertyNames" -> Collections.enumeration(propiedades.keySet());
                    case "getStringProperty" -> propiedades.get((String) args[0]);
                    case "toString" -> "TextMessage[" + cuerpo + "]";
                    default -> null;
                });
    }

    // un mensaje que no es de texto (por ejemplo un BytesMessage)
    public static Message noTexto() {
        return (Message) Proxy.newProxyInstance(MensajesJms.class.getClassLoader(),
                new Class<?>[] { Message.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "getJMSTimestamp" -> 0L;
                    case "getPropertyNames" -> Collections.emptyEnumeration();
                    case "toString" -> "Message";
                    default -> null;
                });
    }

    // un mensaje de texto cuyo cuerpo no se puede leer
    public static TextMessage ilegible() {
        return (TextMessage) Proxy.newProxyInstance(MensajesJms.class.getClassLoader(),
                new Class<?>[] { TextMessage.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "getText", "getJMSTimestamp", "getPropertyNames" -> throw new JMSException("cerrado");
                    case "toString" -> "TextMessage[ilegible]";
                    default -> null;
                });
    }
}
