package tse.cargauy.entities;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

// PostgreSQL no indexa las claves foraneas: sin estos indices, cada busqueda por vehiculo recorre la tabla entera
// (docs/performance.md, primera corrida de la prueba)
@Entity
@Table(indexes = {
        @Index(name = "ix_chofer_ciudadano", columnList = "ciudadano_id"),
        @Index(name = "ix_chofer_empresa", columnList = "empresa_id")})
public class Chofer extends VinculoEmpresa {

    public Chofer() {
    }

    public Chofer(Ciudadano ciudadano, Empresa empresa, LocalDate fechaDesde) {
        super(ciudadano, empresa, fechaDesde);
    }

}
