package tse.cargauy.data.posicion;

import java.util.logging.Logger;

import org.bson.Document;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

// Almacen de Posiciones (DA07): las posiciones de tracking van a MongoDB y no a PostgreSQL, porque son el dato
// que mas crece y solo se agregan. Un solo cliente para toda la aplicacion: MongoClient ya tiene su propio pool
// de conexiones y se puede usar desde varios hilos a la vez
@Singleton
@Startup
public class AlmacenPosiciones {

    private static final Logger LOGGER = Logger.getLogger(AlmacenPosiciones.class.getName());

    static final String COLECCION = "posiciones";

    private MongoClient cliente;
    private MongoCollection<Document> posiciones;
    private volatile boolean conIndices;

    @PostConstruct
    void iniciar() {
        cliente = MongoClients.create(entorno("MONGO_URI", "mongodb://localhost:27017"));
        posiciones = cliente.getDatabase(entorno("MONGO_DB", "cargauy")).getCollection(COLECCION);
        // si MongoDB no esta, el resto del componente central arranca igual: la ingesta falla y el broker
        // reentrega las posiciones hasta que vuelva
        try {
            crearIndices();
        } catch (MongoException e) {
            LOGGER.warning("posiciones sin indices al arrancar, se crean antes de la primera escritura: " + e);
        }
    }

    @PreDestroy
    void cerrar() {
        cliente.close();
    }

    // READ: la coleccion se puede usar desde varios hilos, no hace falta que el contenedor serialice las llamadas.
    // Nunca se devuelve sin el indice unico, porque sin el los duplicados entrarian
    @Lock(LockType.READ)
    public MongoCollection<Document> posiciones() {
        if (!conIndices) {
            crearIndices();
        }
        return posiciones;
    }

    // el indice unico es el que hace idempotente la ingesta: el mismo evento reenviado por el mismo nodo no entra
    // dos veces. Crear un indice que ya existe no hace nada, asi que se puede correr mas de una vez
    private void crearIndices() {
        posiciones.createIndex(Indexes.ascending("idNodo", "idEvento"),
                new IndexOptions().unique(true).name("uk_posicion_origen"));
        posiciones.createIndex(Indexes.ascending("matricula", "timestampGeneracion"),
                new IndexOptions().name("ix_posicion_matricula"));
        conIndices = true;
    }

    private static String entorno(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
