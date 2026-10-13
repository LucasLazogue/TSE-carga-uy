package tse.cargauy.data.fiscalizacion;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// la pista de auditoria es de solo agregado: la base rechaza modificarla o borrarla, aunque sea desde fuera de la aplicacion
@Singleton
@Startup
public class InstaladorAuditoria {

    static final List<String> SENTENCIAS = List.of(
            """
            CREATE OR REPLACE FUNCTION pista_auditoria_inmutable() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN
                RAISE EXCEPTION 'la pista de auditoria es de solo agregado: % no permitido', TG_OP
                    USING ERRCODE = 'insufficient_privilege';
            END;
            $$""",
            """
            CREATE OR REPLACE TRIGGER pista_auditoria_sin_modificar BEFORE UPDATE OR DELETE ON pistaauditoria
                FOR EACH ROW EXECUTE FUNCTION pista_auditoria_inmutable()""",
            """
            CREATE OR REPLACE TRIGGER pista_auditoria_sin_truncate BEFORE TRUNCATE ON pistaauditoria
                FOR EACH STATEMENT EXECUTE FUNCTION pista_auditoria_inmutable()""");

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @PostConstruct
    public void init() {
        for (String sentencia : SENTENCIAS) {
            entityManager.createNativeQuery(sentencia).executeUpdate();
        }
    }
}
