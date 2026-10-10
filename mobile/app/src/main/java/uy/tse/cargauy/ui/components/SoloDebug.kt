package uy.tse.cargauy.ui.components

import androidx.compose.runtime.Composable

import uy.tse.cargauy.BuildConfig

/** Muestra su contenido solo en el build debug; en release no se dibuja. Para herramientas de desarrollo. */
@Composable
fun SoloDebug(content: @Composable () -> Unit) {
    if (BuildConfig.DEBUG) content()
}
