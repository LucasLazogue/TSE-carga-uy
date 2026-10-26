package uy.tse.cargauy.ui.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

import uy.tse.cargauy.data.auth.UsuarioDelToken
import uy.tse.cargauy.data.cedulaLegible
import uy.tse.cargauy.ui.components.Confirmacion
import uy.tse.cargauy.ui.components.MenuLateral
import uy.tse.cargauy.ui.components.OpcionMenu
import uy.tse.cargauy.ui.screens.common.SettingsScreen
import uy.tse.cargauy.ui.screens.login.LoginScreen
import uy.tse.cargauy.ui.screens.viaje.ViajeScreen
import uy.tse.cargauy.ui.screens.viajes.ViajesScreen

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
    val entradaActual by navController.currentBackStackEntryAsState()
    val enViajes = entradaActual?.destination?.hasRoute<Rutas.Viajes>() == true
    val menu = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }

    // al ingresar o al perder la sesion (cerrar sesion o un 401) se va a la pantalla que corresponde,
    // sin dejar historial: con el boton atras no se vuelve al login ni a una pantalla sin sesion
    LaunchedEffect(haySesion) {
        val destino: Any = if (haySesion) Rutas.Viajes else Rutas.Login
        if (navController.currentDestination?.hasRoute(destino::class) != true) {
            navController.navigate(destino) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    BackHandler(enabled = menu.isOpen) { scope.launch { menu.close() } }

    val marco = Marco(
        usuario = if (haySesion) identidad(usuario) else null,
        sesionVencida = haySesion && sesionVencida,
        volver = { navController.popBackStack() },
        abrirMenu = { scope.launch { menu.open() } },
        abrirConfiguracion = { navController.navigate(Rutas.Configuracion) { launchSingleTop = true } }
    )

    ModalNavigationDrawer(
        drawerState = menu,
        // en las demas pantallas, deslizar desde el borde es volver
        gesturesEnabled = haySesion && (enViajes || menu.isOpen),
        drawerContent = {
            if (haySesion) {
                MenuLateral(
                    cuenta = listOfNotNull(usuario?.cedula?.let { "C.I. ${cedulaLegible(it)}" }, usuario?.correo),
                    opciones = listOf(
                        OpcionMenu("Mis viajes", Icons.AutoMirrored.Filled.List, enViajes) {
                            scope.launch { menu.close() }
                            navController.navigate(Rutas.Viajes) { launchSingleTop = true }
                        }
                    ),
                    onCerrarSesion = {
                        scope.launch { menu.close() }
                        confirmarSalida = true
                    }
                )
            }
        }
    ) {
        CompositionLocalProvider(LocalMarco provides marco) {
            NavHost(
                navController = navController,
                startDestination = if (haySesion) Rutas.Viajes else Rutas.Login
            ) {
                destino<Rutas.Login>(Barra("Carga UY", Arriba.NADA, configuracion = true, conSesion = false)) {
                    LoginScreen(ingreso = ingreso, onIngresoProcesado = onIngresoProcesado)
                }
                destino<Rutas.Viajes>(Barra("Mis viajes", Arriba.MENU, configuracion = true)) {
                    ViajesScreen(onAbrirViaje = { idViaje -> navController.navigate(Rutas.Viaje(idViaje)) })
                }
                destino<Rutas.Viaje>(Barra("Viaje")) {
                    ViajeScreen(onVolver = marco.volver)
                }
                destino<Rutas.Configuracion>(Barra("Configuración", conSesion = haySesion)) {
                    SettingsScreen(
                        temaOscuro = temaOscuro,
                        onCambiarTema = onCambiarTema,
                        urlServidor = urlServidor,
                        onGuardarUrl = onGuardarUrl,
                        onRestaurarUrl = onRestaurarUrl
                    )
                }
            }
        }
    }

    if (confirmarSalida) {
        Confirmacion(
            titulo = "¿Cerrar sesión?",
            texto = "Para volver a entrar vas a necesitar conexión. Los eventos sin enviar se envían igual cuando haya señal.",
            textoConfirmar = "Cerrar sesión",
            textoCancelar = "Cancelar",
            onConfirmar = {
                confirmarSalida = false
                onCerrarSesion()
            },
            onCancelar = { confirmarSalida = false }
        )
    }
}

// todavia no hay nombre: lo trae la PDI en el primer ingreso (CU-02)
private fun identidad(usuario: UsuarioDelToken?): String? = when {
    usuario?.cedula != null -> "C.I. ${cedulaLegible(usuario.cedula)}"
    else -> usuario?.correo
}
