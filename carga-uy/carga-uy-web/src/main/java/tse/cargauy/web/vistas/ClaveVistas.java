package tse.cargauy.web.vistas;

import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * revisa al arrancar la clave con la que Mojarra cifra el estado de las pantallas (ver jboss-web.xml). sin clave cada
 * instancia usa una propia y solo anda con una instancia: se avisa. con una clave que Mojarra no puede usar no se
 * cifraria nada y fallarian todas las pantallas: no se despliega.
 */
@WebListener
public class ClaveVistas implements ServletContextListener {

    static final String JNDI = "java:comp/env/faces/ClientSideSecretKey";
    static final String VARIABLE = "CARGAUY_CLAVE_VISTAS";

    private static final Logger LOGGER = Logger.getLogger(ClaveVistas.class.getName());

    enum Estado {
        VALIDA, FALTA, INVALIDA
    }

    @FunctionalInterface
    interface Busqueda {
        String buscar() throws NamingException;
    }

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        revisarYAvisar(() -> (String) new InitialContext().lookup(JNDI));
    }

    static Estado revisarYAvisar(Busqueda busqueda) {
        Estado estado = revisar(leer(busqueda));
        switch (estado) {
            case VALIDA -> LOGGER.info("estado de las pantallas cifrado con la clave de " + VARIABLE);
            case FALTA -> LOGGER.warning(VARIABLE + " no esta definida: esta instancia cifra el estado de las pantallas"
                    + " con una clave propia al azar. con mas de una instancia, el clic que atiende otra no se puede"
                    + " procesar (pagina desactualizada). ver docs/infraestructura.md");
            case INVALIDA -> throw new IllegalStateException(VARIABLE
                    + " no es una clave AES de 16, 24 o 32 bytes en base64 (se genera con openssl rand -base64 32)");
        }
        return estado;
    }

    static Estado revisar(String valor) {
        if (valor == null || valor.isBlank()) {
            return Estado.FALTA;
        }
        try {
            int largo = Base64.getDecoder().decode(valor).length;
            return largo == 16 || largo == 24 || largo == 32 ? Estado.VALIDA : Estado.INVALIDA;
        } catch (IllegalArgumentException e) {
            return Estado.INVALIDA;
        }
    }

    private static String leer(Busqueda busqueda) {
        try {
            return busqueda.buscar();
        } catch (NamingException e) {
            // sin valor, wildfly no publica la entrada
            LOGGER.log(Level.FINE, "sin clave de las pantallas en JNDI", e);
            return null;
        }
    }
}
