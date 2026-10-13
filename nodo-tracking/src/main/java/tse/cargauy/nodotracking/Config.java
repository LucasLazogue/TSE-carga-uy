package tse.cargauy.nodotracking;

// configuracion del nodo; se lee de variables de entorno
public record Config(String nodoId, int puerto, String jmsUrl, String jmsUsuario, String jmsClave, int maxLote) {

    public static Config desdeEntorno() {
        Config config = new Config(
            obligatoria("NODO_ID"),
            numero("NODO_PUERTO", 8081),
            // con TLS (R8): el certificado del broker de desarrollo esta en tls/confianza.p12
            texto("NODO_JMS_URL", "tcp://localhost:61617?sslEnabled=true&trustStorePath=tls/confianza.p12&trustStorePassword=cargauy-dev"),
            obligatoria("NODO_JMS_USUARIO"),
            obligatoria("NODO_JMS_CLAVE"),
            numero("NODO_MAX_LOTE", 500)
        );
        return config;
    }

    static String obligatoria(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + nombre);
        }
        return valor;
    }

    static String texto(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        return valor;
    }

    static int numero(String nombre, int porDefecto) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("La variable de entorno " + nombre + " debe ser un número entero", e);
        }
    }

    static double decimal(String nombre, double porDefecto) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("La variable de entorno " + nombre + " debe ser un número", e);
        }
    }
}
