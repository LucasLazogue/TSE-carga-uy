package uy.tse.cargauy.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

import uy.tse.cargauy.ui.components.Aviso
import uy.tse.cargauy.ui.components.Header
import uy.tse.cargauy.ui.theme.Spacing

enum class Arriba { NADA, VOLVER, MENU }

data class Barra(
    val titulo: String,
    val arriba: Arriba = Arriba.VOLVER,
    val configuracion: Boolean = false,
    val conSesion: Boolean = true
)

// lo que necesita la barra de cualquier pantalla; lo arma NavGraph una sola vez
class Marco(
    val usuario: String?,
    val sesionVencida: Boolean,
    val volver: () -> Unit,
    val abrirMenu: () -> Unit,
    val abrirConfiguracion: () -> Unit
)

val LocalMarco = compositionLocalOf<Marco> { error("las pantallas se registran dentro de NavGraph") }

inline fun <reified T : Any> NavGraphBuilder.destino(barra: Barra, noinline contenido: @Composable (T) -> Unit) {
    composable<T> { entrada -> ConBarra(barra) { contenido(entrada.toRoute<T>()) } }
}

@Composable
fun ConBarra(barra: Barra, contenido: @Composable () -> Unit) {
    val marco = LocalMarco.current
    Scaffold(
        topBar = {
            Header(
                titulo = barra.titulo,
                subtitulo = marco.usuario.takeIf { barra.conSesion },
                onVolver = marco.volver.takeIf { barra.arriba == Arriba.VOLVER },
                onMenu = marco.abrirMenu.takeIf { barra.arriba == Arriba.MENU },
                onSettings = marco.abrirConfiguracion.takeIf { barra.configuracion }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // sin boton para ingresar: sin señal lo dejaria afuera. con señal, el primer pedido recibe 401 y vuelve al login
            if (barra.conSesion && marco.sesionVencida) {
                Aviso(
                    texto = "Tu sesión venció. Podés seguir viendo tu viaje y registrando eventos sin conexión. " +
                        "Cuando tengas señal, la app te va a pedir que ingreses de nuevo y ahí se envían los eventos.",
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                )
            }
            Box(modifier = Modifier.weight(1f)) { contenido() }
        }
    }
}
