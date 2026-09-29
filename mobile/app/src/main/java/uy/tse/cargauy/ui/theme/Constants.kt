package uy.tse.cargauy.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tamaños de letra. Usar solo dentro de Type.kt. */
object FontSize {
    val xs = 12.sp
    val sm = 14.sp
    val md = 16.sp
    val lg = 18.sp
    val xl = 22.sp
    val xxl = 28.sp
}

/** Altura de línea. Regla práctica: ~1.4x el tamaño de letra. */
object LineHeight {
    val xs = 16.sp
    val sm = 20.sp
    val md = 24.sp
    val lg = 26.sp
    val xl = 28.sp
    val xxl = 36.sp
}

/** Espaciados: paddings, márgenes, separación entre elementos. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/** Radios de esquina. */
object Radius {
    val sm = 4.dp
    val md = 8.dp
    val lg = 16.dp
    val full = 999.dp
}

/** Grosores de borde y divisores. */
object Thickness {
    val hairline = 1.dp
    val thin = 2.dp
}