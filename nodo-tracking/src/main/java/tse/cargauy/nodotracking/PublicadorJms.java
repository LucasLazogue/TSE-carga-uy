package tse.cargauy.nodotracking;

import java.net.URI;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Queue;

// publica en queue_tracking del componente central con el cliente JMS remoto de WildFly
public class PublicadorJms implements Publicador, AutoCloseable {

    private final Config config;
    private ActiveMQJMSConnectionFactory fabrica;
    // null = desconectado. volatile: /health lo lee desde otro hilo sin tomar el lock
    private volatile JMSContext principal;
    private Queue cola;

    public PublicadorJms(Config config) {
        this.config = config;
    }

    @Override
    public void publicar(List<String> mensajes) throws PublicadorException {
        try (JMSContext sesion = nuevaSesion()) {
            var productor = sesion.createProducer();
            for (String mensaje: mensajes) {
                productor.send(cola, mensaje);
            }
            sesion.commit();
        } catch (JMSRuntimeException e) {
            desconectar();
            throw new PublicadorException("No se pudo publicar en el broker", e);
        }
    }

    // no intenta conectar: con el central caido un intento puede quedar colgado hasta el timeout de TCP, y /health
    // no responderia. la reconexion la hace el hilo que publica en cada reintento
    @Override
    public boolean disponible() {
        return principal != null;
    }

    private synchronized JMSContext nuevaSesion() throws PublicadorException {
        conectar();
        return principal.createContext(JMSContext.SESSION_TRANSACTED);
    }

    private synchronized void conectar() throws PublicadorException {
        if (principal != null) {
            return; // ya conectado
        }
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        props.put(Context.PROVIDER_URL, config.jmsUrl());
        props.put(Context.SECURITY_PRINCIPAL, config.jmsUsuario());
        props.put(Context.SECURITY_CREDENTIALS, config.jmsClave());
        
        InitialContext ctx = null;
        try {
            ctx = new InitialContext(props);
            cola = (Queue) ctx.lookup("jms/queue/queue_tracking");
            fabrica = new ActiveMQJMSConnectionFactory(urlBroker());
            principal = fabrica.createContext(config.jmsUsuario(), config.jmsClave());
            // el cliente avisa cuando se cae la conexion: sin esto /health diria UP hasta que falle un envio
            principal.setExceptionListener(e -> desconectar());
        } catch (NamingException | JMSRuntimeException e) {
            desconectar();
            throw new PublicadorException("No se pudo conectar al broker", e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ignorada) {
                    // nada que hacer
                }
            }
        }
    }

    // el RemoteConnectionFactory del JNDI apunta al host que anuncia WildFly (localhost en el compose), que desde
    // otro contenedor o PaaS no es el central. por eso el factory se arma con el mismo host y puerto de NODO_JMS_URL
    private String urlBroker() {
        URI destino = URI.create(config.jmsUrl());
        int puerto = destino.getPort() == -1 ? 8080 : destino.getPort();
        return "tcp://" + destino.getHost() + ":" + puerto + "?httpUpgradeEnabled=true&httpUpgradeEndpoint=http-acceptor";
    }

    // se llama despues de un error: el proximo publicar vuelve a conectar
    private synchronized void desconectar() {
        if (principal != null) {
            try {
                principal.close();
            } catch (JMSRuntimeException ignorada) {
                // la conexion ya estaba rota: no hay nada mas que cerrar
            }
            principal = null;
        }
        if (fabrica != null) {
            fabrica.close();
            fabrica = null;
        }
    }

    @Override
    public void close() {
        desconectar();
    }
}