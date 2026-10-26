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
    val xxs = 2.dp
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
    val lg = 12.dp
    val xl = 16.dp
    val xxl = 28.dp
    val full = 999.dp
}

// texto secundario sobre la barra de color, que no tiene un rol on*Variant
const val OPACIDAD_SECUNDARIA = 0.8f

const val FRACCION_MENU_LATERAL = 0.7f

/** Grosores de borde y divisores. */
object Thickness {
    val hairline = 1.dp
    val thin = 2.dp
}

/** Tamaños fijos de componentes. */
object Sizes {
    // area tactil minima de material
    val tactil = 48.dp
    val icono = 24.dp
    val iconoChico = 18.dp
    // minimo: con la letra grande del sistema crece
    val columnaHora = 76.dp
    // en tablets o en horizontal el contenido no se estira de borde a borde (ventana compacta de material)
    val anchoMaximoContenido = 600.dp
    // las listas ponen tantas columnas como entren de este ancho: una en un telefono, dos o mas en tablet
    val anchoMinimoColumna = 320.dp
}