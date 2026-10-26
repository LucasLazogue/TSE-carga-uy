package uy.tse.cargauy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ParDeColores(val fondo: Color, val contenido: Color)

// cuando usar cada uno: doc/codigo/mobile-ui.md
@Immutable
data class ColoresSemanticos(
    val exito: ParDeColores,
    val aviso: ParDeColores,
    val error: ParDeColores,
    val info: ParDeColores,
    val neutro: ParDeColores,
    val destacado: ParDeColores
)

val LocalColoresSemanticos = staticCompositionLocalOf {
    val vacio = ParDeColores(Color.Unspecified, Color.Unspecified)
    ColoresSemanticos(vacio, vacio, vacio, vacio, vacio, vacio)
}

val MaterialTheme.coloresSemanticos: ColoresSemanticos
    @Composable @ReadOnlyComposable get() = LocalColoresSemanticos.current

fun coloresSemanticos(esquema: ColorScheme, darkTheme: Boolean) = ColoresSemanticos(
    exito = if (darkTheme) ParDeColores(ExitoContainerDark, OnExitoContainerDark)
    else ParDeColores(ExitoContainerLight, OnExitoContainerLight),
    aviso = if (darkTheme) ParDeColores(AvisoContainerDark, OnAvisoContainerDark)
    else ParDeColores(AvisoContainerLight, OnAvisoContainerLight),
    error = ParDeColores(esquema.errorContainer, esquema.onErrorContainer),
    info = ParDeColores(esquema.tertiaryContainer, esquema.onTertiaryContainer),
    neutro = ParDeColores(esquema.surfaceContainerHighest, esquema.onSurfaceVariant),
    destacado = ParDeColores(esquema.primaryContainer, esquema.onPrimaryContainer)
)
