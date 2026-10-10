package uy.tse.cargauy.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import uy.tse.cargauy.ui.components.Header
import uy.tse.cargauy.ui.screens.common.SettingsScreen
import uy.tse.cargauy.ui.screens.inicio.InicioScreen

object Rutas {
    const val INICIO = "inicio"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(
    temaOscuro: Boolean,
    onCambiarTema: (Boolean) -> Unit,
    urlServidor: String,
    onGuardarUrl: (String) -> Unit,
    onRestaurarUrl: () -> Unit
) {
    val navController = rememberNavController()
    val rutaActual by navController.currentBackStackEntryAsState()
    val ruta = rutaActual?.destination?.route

    Scaffold(
        topBar = {
            when (ruta) {
                Rutas.INICIO -> Header(
                    titulo = "Carga UY",
                    onSettings = { navController.navigate(Rutas.SETTINGS) }
                )
                Rutas.SETTINGS -> Header(
                    titulo = "Configuración",
                    onVolver = { navController.popBackStack() }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Rutas.INICIO,
            modifier = Modifier.padding(padding)
        ) {
            composable(Rutas.INICIO) {
                InicioScreen()
            }
            composable(Rutas.SETTINGS) {
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