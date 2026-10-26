package uy.tse.cargauy.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument

import uy.tse.cargauy.data.auth.UsuarioDelToken
import uy.tse.cargauy.data.cedulaLegible
import uy.tse.cargauy.ui.components.Aviso
import uy.tse.cargauy.ui.components.Header
import uy.tse.cargauy.ui.screens.common.SettingsScreen
import uy.tse.cargauy.ui.screens.login.LoginScreen
import uy.tse.cargauy.ui.screens.viaje.ViajeScreen
import uy.tse.cargauy.ui.screens.viaje.ViajeViewModel
import uy.tse.cargauy.ui.screens.viajes.ViajesScreen
import uy.tse.cargauy.ui.theme.Spacing

object Rutas {
    const val LOGIN = "login"
    const val VIAJES = "viajes"
    const val VIAJE = "viaje/{${ViajeViewModel.ARGUMENTO}}"
    const val SETTINGS = "settings"

    fun viaje(idViaje: Long) = "viaje/$idViaje"
}

@Composable
fun NavGraph(
    temaOscuro: Boolean,
    onCambiarTema: (Boolean) -> Unit,
    urlServidor: String,
    onGuardarUrl: (String) -> Unit,
    onRestaurarUrl: () -> Unit,
    haySesion: Boolean?,
    usuario: UsuarioDelToken?,
    sesionVencida: Boolean,
    ingreso: Uri?,
    onIngresoProcesado: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    // mientras se lee la sesion del disco no se dibuja nada (es un instante)
    if (haySesion == null) return

    val navController = rememberNavController()
    val rutaActual by navController.currentBackStackEntryAsState()
    val ruta = rutaActual?.destination?.route
    val logueado = if (haySesion) identidad(usuario) else null

    // al ingresar o al perder la sesion (cerrar sesion o un 401) se va a la pantalla que corresponde,
    // sin dejar historial: con el boton atras no se vuelve al login ni a una pantalla sin sesion
    LaunchedEffect(haySesion) {
        val destino = if (haySesion) Rutas.VIAJES else Rutas.LOGIN
        if (navController.currentDestination?.route != destino) {
            navController.navigate(destino) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            when (ruta) {
                Rutas.LOGIN -> Header(
                    titulo = "Carga UY",
                    onSettings = { navController.navigate(Rutas.SETTINGS) }
                )
                Rutas.VIAJES -> Header(
                    titulo = "Mis viajes",
                    subtitulo = logueado,
                    onSettings = { navController.navigate(Rutas.SETTINGS) }
                )
                Rutas.VIAJE -> Header(
                    titulo = "Viaje",
                    subtitulo = logueado,
                    onVolver = { navController.popBackStack() }
                )
                Rutas.SETTINGS -> Header(
                    titulo = "Configuración",
                    subtitulo = logueado,
                    onVolver = { navController.popBackStack() }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // arriba de cualquier pantalla con sesion: el chofer sigue trabajando, pero tiene que saber que pasa despues.
            // sin boton para ingresar: sin señal lo dejaria afuera. con señal, el primer pedido recibe 401 y vuelve al login
            if (haySesion && sesionVencida) {
                Aviso(
                    texto = "Tu sesión venció. Podés seguir viendo tu viaje y registrando eventos sin conexión. " +
                        "Cuando tengas señal, la app te va a pedir que ingreses de nuevo y ahí se envían los eventos.",
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                )
            }
            NavHost(
                navController = navController,
                startDestination = if (haySesion) Rutas.VIAJES else Rutas.LOGIN,
                modifier = Modifier.weight(1f)
            ) {
                composable(Rutas.LOGIN) {
                    LoginScreen(
                        ingreso = ingreso,
                        onIngresoProcesado = onIngresoProcesado
                    )
                }
                composable(Rutas.VIAJES) {
                    ViajesScreen(onAbrirViaje = { idViaje -> navController.navigate(Rutas.viaje(idViaje)) })
                }
                composable(
                    route = Rutas.VIAJE,
                    arguments = listOf(navArgument(ViajeViewModel.ARGUMENTO) { type = NavType.LongType })
                ) {
                    ViajeScreen(onVolver = { navController.popBackStack() })
                }
                composable(Rutas.SETTINGS) {
                    SettingsScreen(
                        temaOscuro = temaOscuro,
                        onCambiarTema = onCambiarTema,
                        urlServidor = urlServidor,
                        onGuardarUrl = onGuardarUrl,
                        onRestaurarUrl = onRestaurarUrl,
                        usuario = if (haySesion) usuario else null,
                        onCerrarSesion = if (haySesion) onCerrarSesion else null
                    )
                }
            }
        }
    }
}

// todavia no hay nombre: lo trae la PDI en el primer ingreso (CU-02)
private fun identidad(usuario: UsuarioDelToken?): String? = when {
    usuario?.cedula != null -> "C.I. ${cedulaLegible(usuario.cedula)}"
    else -> usuario?.correo
}
