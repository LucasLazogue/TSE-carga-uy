package tse.cargauy.data.testdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tse.cargauy.data.BaseDatos;
import tse.cargauy.data.empresa.EmpresaDAO;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.dtos.Paginacion;

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
}
