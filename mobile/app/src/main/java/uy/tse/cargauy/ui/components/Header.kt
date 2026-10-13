package uy.tse.cargauy.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

import uy.tse.cargauy.ui.theme.LocalColoresBarra

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
) {
    val barra = LocalColoresBarra.current

    TopAppBar(
        title = {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            onVolver?.let { action ->
                IconButton(onClick = action) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                    )
                }
            }
        },
        actions = {
            onSettings?.let { action ->
                IconButton(onClick = action) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Configuración",
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = barra.fondo,
            titleContentColor = barra.contenido,
            navigationIconContentColor = barra.contenido,
            actionIconContentColor = barra.contenido,
        ),
    )
}