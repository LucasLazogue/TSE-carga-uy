package uy.tse.cargauy.ui.screens.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import uy.tse.cargauy.ui.components.Contenido
import uy.tse.cargauy.ui.components.Subtitulo
import uy.tse.cargauy.ui.theme.Spacing

@Composable
fun InicioScreen(
    viewModel: InicioViewModel = viewModel()
) {
    val eventosPendientes by viewModel.eventosPendientes.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Subtitulo(texto = "Bienvenido a Carga UY")
        Contenido(texto = "Eventos pendientes de sincronizar: $eventosPendientes")
    }
}
