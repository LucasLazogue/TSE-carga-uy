package tse.cargauy.nodobalanzas.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

import jakarta.persistence.EntityManager;
import tse.cargauy.nodobalanzas.data.peaje.PeajeDAO;
import tse.cargauy.nodobalanzas.data.pesaje.PesajeDAO;
import tse.cargauy.nodobalanzas.dtos.BalanzaDto;
import tse.cargauy.nodobalanzas.dtos.PeajeDto;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;
import tse.cargauy.nodobalanzas.entities.Balanza;
import tse.cargauy.nodobalanzas.entities.Peaje;
import tse.cargauy.nodobalanzas.entities.Pesaje;
import tse.cargauy.nodobalanzas.entities.Sentido;
import tse.cargauy.nodobalanzas.seguridad.Sha256;

// los DAO del nodo contra un PostgreSQL real (Testcontainers), con el esquema generado de las entidades. Cada test
// corre en una transaccion que se deshace al terminar
class BalanzasDAOTest {

    private static PostgreSQLContainer postgres;
    private static SessionFactory fabrica;

    private EntityManager em;
    private final PeajeDAO peajes = new PeajeDAO();
    private final PesajeDAO pesajes = new PesajeDAO();

    @BeforeAll
    static void arrancar() {
        // sin Docker se saltean en una maquina de desarrollo; en el CI (CI=true) es un error
        boolean hayDocker = DockerClientFactory.instance().isDockerAvailable();
        if ("true".equals(System.getenv("CI")) && !hayDocker) {
            throw new IllegalStateException("en el CI se necesita Docker para probar los DAO contra la base");
        }
        assumeTrue(hayDocker, "sin Docker no se prueban los DAO contra la base");
        postgres = new PostgreSQLContainer("postgres:17");
        postgres.start();
        fabrica = new Configuration()
                .setProperty("hibernate.connection.url", postgres.getJdbcUrl())
                .setProperty("hibernate.connection.username", postgres.getUsername())
                .setProperty("hibernate.connection.password", postgres.getPassword())
                .setProperty("hibernate.hbm2ddl.auto", "create")
                .addAnnotatedClass(Peaje.class)
                .addAnnotatedClass(Balanza.class)
                .addAnnotatedClass(Pesaje.class)
                .buildSessionFactory();
    }

    @BeforeEach
    void abrir() throws ReflectiveOperationException {
        em = fabrica.createEntityManager();
        em.getTransaction().begin();
        for (Object dao : List.of(peajes, pesajes)) {
            Field campo = dao.getClass().getDeclaredField("entityManager");
            campo.setAccessible(true);
            campo.set(dao, em);
        }
        peajes.addPeaje(new PeajeDto("r5-km30", "Peaje ruta 5 km 30", "5", 30, -34.6, -56.2));
        peajes.addBalanza("r5-km30", new BalanzaDto("balanza-r5-km30-n", Sentido.HACIA_MONTEVIDEO, null), Sha256.base64Url("clave-n"));
        peajes.addBalanza("r5-km30", new BalanzaDto("balanza-r5-km30-s", Sentido.HACIA_EL_INTERIOR, null), Sha256.base64Url("clave-s"));
        sincronizar();
    }

    @AfterEach
    void cerrar() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        em.close();
    }

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    @Test
    void peajesConSusBalanzas() {
        List<PeajeDto> todos = peajes.getPeajes();

        assertEquals(1, todos.size());
        assertEquals("Peaje ruta 5 km 30", todos.get(0).getNombre());
        assertEquals(2, todos.get(0).getBalanzas().size());
        assertEquals("5", peajes.getPeajeByCodigo("r5-km30").getRuta());
        assertNull(peajes.getPeajeByCodigo("no-existe"));
        assertEquals(Sentido.HACIA_EL_INTERIOR, peajes.getBalanzaByCodigo("balanza-r5-km30-s").getSentido());
        assertNull(peajes.getBalanzaByCodigo("no-existe"));
    }

    // cada balanza se autentica con su clave; el nodo guarda solo el hash
    @Test
    void balanzaPorElHashDeSuClave() {
        assertEquals("balanza-r5-km30-n", peajes.getCodigoBalanzaByClave(Sha256.base64Url("clave-n")));
        assertNull(peajes.getCodigoBalanzaByClave(Sha256.base64Url("otra")));
        assertNull(peajes.getBalanzaByCodigo("balanza-r5-km30-n").getClave(), "la clave no sale del nodo");
    }

    @Test
    void pesajesPorMatriculaEnUnRango() {
        LocalDateTime dia = LocalDateTime.of(2026, 10, 20, 9, 0);
        pesajes.addPesaje("balanza-r5-km30-n", new PesajeDto("1001", "STA1234", dia, 33000), dia.plusMinutes(1));
        pesajes.addPesaje("balanza-r5-km30-s", new PesajeDto("1002", "STA1234", dia.plusHours(6), 21000), dia.plusHours(6));
        pesajes.addPesaje("balanza-r5-km30-n", new PesajeDto("1003", "SBB5678", dia, 30000), dia);
        sincronizar();

        List<PesajeDto> deSta = pesajes.getPesajesByMatricula("STA1234", dia.minusHours(1), dia.plusHours(1));
        assertEquals(1, deSta.size());
        assertEquals(33000, deSta.get(0).getPesoRegistrado());
        assertEquals("balanza-r5-km30-n", deSta.get(0).getBalanza().getCodigo());
        assertEquals("r5-km30", deSta.get(0).getPeaje().getCodigo());
        assertEquals(2, pesajes.getPesajesByMatricula("STA1234", dia.minusHours(1), dia.plusDays(1)).size());
        assertTrue(pesajes.getPesajesByMatricula("STA1234", dia.plusDays(1), dia.plusDays(2)).isEmpty());
    }

    // la balanza reenvia lo que no le confirmaron: el mismo registro de la misma balanza ya esta
    @Test
    void registroYaRecibido() {
        LocalDateTime dia = LocalDateTime.of(2026, 10, 20, 9, 0);
        pesajes.addPesaje("balanza-r5-km30-n", new PesajeDto("1001", "STA1234", dia, 33000), dia);
        sincronizar();

        assertTrue(pesajes.existePesaje("balanza-r5-km30-n", "1001"));
        assertFalse(pesajes.existePesaje("balanza-r5-km30-s", "1001"), "otra balanza puede tener el mismo numero");
        assertFalse(pesajes.existePesaje("balanza-r5-km30-n", "1002"));
    }
}
