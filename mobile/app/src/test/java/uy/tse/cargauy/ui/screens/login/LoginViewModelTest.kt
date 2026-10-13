package uy.tse.cargauy.ui.screens.login

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginViewModelTest {

    @Test
    fun mensajeLogin_traduceLosCodigosDelCentral() {
        assertEquals("El ingreso no es válido o expiró. Volvé a intentarlo.", LoginViewModel.mensajeLogin("4002"))
        assertEquals("gub.uy no está disponible. Probá de nuevo más tarde.", LoginViewModel.mensajeLogin("4004"))
    }

    @Test
    fun mensajeLogin_codigoDesconocido() {
        assertEquals("No se pudo ingresar (código 9999).", LoginViewModel.mensajeLogin("9999"))
        assertEquals("No se pudo ingresar (código desconocido).", LoginViewModel.mensajeLogin(null))
    }
}
