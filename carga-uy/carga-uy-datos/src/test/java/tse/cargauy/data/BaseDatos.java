package tse.cargauy.data;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.postgresql.PostgreSQLContainer;

import jakarta.ejb.EJB;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.data.testdata.DatosPrueba;

// pruebas de los DAO contra un PostgreSQL real (Testcontainers), con el esquema que Hibernate genera de las entidades
// igual que en el servidor. Un solo contenedor para toda la suite, sembrado una vez con los datos de la demo
// (DatosPrueba). Cada test corre en una transaccion que se deshace al terminar, asi no se pisan entre si.
// Los EJB se arman como lo haria el contenedor: @PersistenceContext recibe el EntityManager del test y cada @EJB el
// bean del tipo pedido
public abstract class BaseDatos {

    private static PostgreSQLContainer postgres;
    private static SessionFactory fabrica;

    protected EntityManager em;
    private final Map<Class<?>, Object> beans = new HashMap<>();

    @BeforeAll
    static void arrancar() {
        Contenedores.exigirDocker();
        if (fabrica != null) {
            return;
        }
        postgres = new PostgreSQLContainer(Contenedores.POSTGRES);
        postgres.start();
        Configuration configuracion = new Configuration()
                .setProperty("hibernate.connection.url", postgres.getJdbcUrl())
                .setProperty("hibernate.connection.username", postgres.getUsername())
                .setProperty("hibernate.connection.password", postgres.getPassword())
                .setProperty("hibernate.hbm2ddl.auto", "create")
                .setProperty("hibernate.connection.pool_size", "4");
        for (Class<?> entidad : entidades()) {
            configuracion.addAnnotatedClass(entidad);
        }
        fabrica = configuracion.buildSessionFactory();
        sembrar();
    }

    private static void sembrar() {
        BaseDatos semilla = new BaseDatos() {
        };
        semilla.em = fabrica.createEntityManager();
        semilla.em.getTransaction().begin();
        semilla.bean(DatosPrueba.class).init();
        semilla.em.getTransaction().commit();
        semilla.em.close();
    }

    @BeforeEach
    void abrir() {
        em = fabrica.createEntityManager();
        em.getTransaction().begin();
    }

    @AfterEach
    void cerrar() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        em.close();
    }

    // lo escrito hasta ahora va a la base (sigue dentro de la transaccion del test) y las consultas siguientes leen
    // de la base y no de lo que el EntityManager tiene en memoria
    protected void sincronizar() {
        em.flush();
        em.clear();
    }

    // el bean de la clase pedida, con sus dependencias; uno por test, como un EJB por transaccion
    @SuppressWarnings("unchecked")
    protected <T> T bean(Class<T> clase) {
        Object existente = beans.get(clase);
        if (existente != null) {
            return (T) existente;
        }
        try {
            T bean = clase.getDeclaredConstructor().newInstance();
            beans.put(clase, bean);
            inyectar(bean, clase);
            return bean;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no se pudo armar " + clase.getName(), e);
        }
    }

    private void inyectar(Object bean, Class<?> clase) throws ReflectiveOperationException {
        for (Class<?> c = clase; c != Object.class; c = c.getSuperclass()) {
            for (Field campo : c.getDeclaredFields()) {
                Object valor = null;
                if (campo.isAnnotationPresent(PersistenceContext.class)) {
                    valor = em;
                } else if (campo.isAnnotationPresent(EJB.class)) {
                    valor = bean(implementacion(campo.getType()));
                }
                if (valor != null) {
                    campo.setAccessible(true);
                    campo.set(bean, valor);
                }
            }
        }
    }

    // para una interfaz Local, la clase del mismo paquete que la implementa (EmpresaDAOLocal -> EmpresaDAO)
    private static Class<?> implementacion(Class<?> tipo) throws ClassNotFoundException {
        if (!tipo.isInterface()) {
            return tipo;
        }
        String nombre = tipo.getName().replaceAll("(Local|Remote)$", "");
        Class<?> clase = Class.forName(nombre);
        if (!tipo.isAssignableFrom(clase)) {
            throw new IllegalStateException(nombre + " no implementa " + tipo.getName());
        }
        return clase;
    }

    private static List<Class<?>> entidades() {
        String paquete = "tse.cargauy.entities";
        URL url = BaseDatos.class.getClassLoader().getResource(paquete.replace('.', '/'));
        List<Class<?>> entidades = new ArrayList<>();
        try {
            File[] archivos = new File(url.toURI()).listFiles((dir, nombre) -> nombre.endsWith(".class") && !nombre.contains("$"));
            for (File archivo : archivos) {
                Class<?> clase = Class.forName(paquete + "." + archivo.getName().replace(".class", ""));
                if (clase.isAnnotationPresent(Entity.class) || clase.isAnnotationPresent(Embeddable.class)
                        || clase.isAnnotationPresent(MappedSuperclass.class)) {
                    entidades.add(clase);
                }
            }
        } catch (URISyntaxException | ClassNotFoundException e) {
            throw new IllegalStateException("no se pudieron leer las entidades", e);
        }
        return entidades;
    }
}
