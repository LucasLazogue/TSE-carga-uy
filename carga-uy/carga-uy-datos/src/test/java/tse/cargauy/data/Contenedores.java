package tse.cargauy.data;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.testcontainers.DockerClientFactory;

// lo comun a las pruebas contra bases reales en contenedores (Testcontainers)
public final class Contenedores {

    // las mismas imagenes que el docker-compose y OpenShift
    public static final String POSTGRES = "postgres:17";
    public static final String MONGO = "mongo:8.0";

    private Contenedores() {
    }

    // en una maquina sin Docker (por ejemplo sin Docker Desktop) las pruebas se saltean. En el CI no: GitHub Actions
    // define CI=true, y ahi sin Docker es un error, para que la cobertura de los DAO no se pierda sin que nadie lo note
    public static void exigirDocker() {
        boolean hayDocker = DockerClientFactory.instance().isDockerAvailable();
        if ("true".equals(System.getenv("CI")) && !hayDocker) {
            throw new IllegalStateException("en el CI se necesita Docker para probar los DAO contra la base");
        }
        assumeTrue(hayDocker, "sin Docker no se prueban los DAO contra la base");
    }
}
