package tse.cargauy.data.testdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import tse.cargauy.data.BaseDatos;
import tse.cargauy.data.empresa.EmpresaDAO;
import tse.cargauy.data.guia.GuiaDAO;
import tse.cargauy.data.informe.InformeDAO;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.ViajesPorPeriodoDto;

// la demo se carga sobre una base vacia y volver a arrancar no duplica nada
class SemillaDemoTest extends BaseDatos {

    @Test
    void cargaElElencoDeLaDemo() {
        assertNotNull(bean(VehiculoDAO.class).getVehiculoByMatricula("STA1234"));
        assertNotNull(bean(UsuarioDAO.class).getFuncionarioByCedula("11111111"));
        assertNotNull(bean(UsuarioDAO.class).getCiudadanoByCedula("55555555"));
        assertTrue(bean(EmpresaDAO.class).getAll(new Paginacion(0, 50)).getTotal() >= 3);
    }

    @Test
    void arrancarDeNuevoNoDuplica() {
        long empresas = bean(EmpresaDAO.class).getAll(new Paginacion(0, 1)).getTotal();
        long vehiculos = bean(VehiculoDAO.class).getAll(new Paginacion(0, 1)).getTotal();

        bean(DatosPrueba.class).init();
        sincronizar();

        assertEquals(empresas, bean(EmpresaDAO.class).getAll(new Paginacion(0, 1)).getTotal());
        assertEquals(vehiculos, bean(VehiculoDAO.class).getAll(new Paginacion(0, 1)).getTotal());
    }

    // los reportes de las autoridades: viajes casi todos los dias y, con cualquier tipo de carga, infracciones
    @Test
    void losReportesTienenDatosConCualquierTipoDeCarga() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        InformeDAO informes = bean(InformeDAO.class);

        long viajes = informes.contarViajesPorDia(new FiltroInforme(desde, hasta, null)).stream()
                .mapToLong(ViajesPorPeriodoDto::getCantidad).sum();
        assertTrue(viajes > 500, "viajes: " + viajes);
        for (TipoCargaDto tipo : bean(GuiaDAO.class).getTiposCarga()) {
            long infracciones = informes.contarInfracciones(new FiltroInforme(desde, hasta, tipo.getId())).stream()
                    .mapToLong(InfraccionesPorTipoDto::getDetectadas).sum();
            assertTrue(infracciones > 0, tipo.getNombre());
        }
        // las notificaciones de esa historia quedan leidas
        assertEquals(0L, em.createQuery("SELECT COUNT(n) FROM Notificacion n WHERE n.leida = false AND n.caso.vehiculo.matricula"
                + " LIKE :historia", Long.class).setParameter("historia", HistoriaReportes.PREFIJO_MATRICULAS + "%").getSingleResult());
    }
}
