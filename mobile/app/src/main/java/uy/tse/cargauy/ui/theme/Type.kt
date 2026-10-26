package uy.tse.cargauy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// la escala completa: un rol sin definir usaria los tamaños por defecto de material
private fun estilo(peso: FontWeight, tamanio: TextUnit, altura: TextUnit, espaciado: Float) =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = peso,
        fontSize = tamanio,
        lineHeight = altura,
        letterSpacing = espaciado.sp
    )

val Typography = Typography(
    headlineMedium = estilo(FontWeight.SemiBold, FontSize.xxl, LineHeight.xxl, 0f),
    headlineSmall = estilo(FontWeight.SemiBold, FontSize.xl, LineHeight.xl, 0f),
    titleLarge = estilo(FontWeight.SemiBold, FontSize.xl, LineHeight.xl, 0f),
    titleMedium = estilo(FontWeight.Medium, FontSize.lg, LineHeight.md, 0.15f),
    titleSmall = estilo(FontWeight.Medium, FontSize.md, LineHeight.md, 0.1f),
    bodyLarge = estilo(FontWeight.Normal, FontSize.md, LineHeight.md, 0.5f),
    bodyMedium = estilo(FontWeight.Normal, FontSize.sm, LineHeight.sm, 0.25f),
    bodySmall = estilo(FontWeight.Normal, FontSize.xs, LineHeight.xs, 0.4f),
    labelLarge = estilo(FontWeight.Medium, FontSize.sm, LineHeight.sm, 0.1f),
    labelMedium = estilo(FontWeight.Medium, FontSize.xs, LineHeight.xs, 0.5f),
    labelSmall = estilo(FontWeight.Medium, FontSize.xs, LineHeight.xs, 0.5f)
)
