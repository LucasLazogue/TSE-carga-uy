package tse.cargauy.nodobalanzas.ws.pesajes;

import java.time.LocalDate;
import java.util.List;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import tse.cargauy.nodobalanzas.dtos.PesajeDto;
import tse.cargauy.nodobalanzas.negocio.pesaje.PesajeEJBLocal;

@RequestScoped
@Path("/pesajes")
@Produces("application/json")
public class PesajesRest {

    @EJB
    PesajeEJBLocal pesajeEJB;

    public PesajesRest() {
    }

    @GET
    public List<PesajeDto> getPesajes(@QueryParam("matricula") String matricula, @QueryParam("rut") String rut,
            @QueryParam("fecha") LocalDate fecha) {
        return pesajeEJB.getPesajes(matricula, rut, fecha);
    }
}
