package uy.tse.cargauy.ui.screens.login

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

import uy.tse.cargauy.ui.UiState
import uy.tse.cargauy.ui.components.Aviso
import uy.tse.cargauy.ui.components.Descripcion
import uy.tse.cargauy.ui.components.SoloDebug
import uy.tse.cargauy.ui.components.TextoError
import uy.tse.cargauy.ui.components.TituloSeccion
import uy.tse.cargauy.ui.theme.Sizes
import uy.tse.cargauy.ui.theme.Spacing

@Composable
fun LoginScreen(
    ingreso: Uri?,
    onIngresoProcesado: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val sesionRechazada by viewModel.sesionRechazada.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // TODO mock: para probar con otros usuarios del login simulado; vacia entra como Carlos (55555555)
    var cedulaPrueba by remember { mutableStateOf("") }

    // la vuelta del navegador: se canjea una vez y se avisa a MainActivity para que no se repita
    LaunchedEffect(ingreso) {
        if (ingreso != null) {
            viewModel.canjear(ingreso)
            onIngresoProcesado()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentWidth()
            .widthIn(max = Sizes.anchoMaximoContenido)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (sesionRechazada) {
            Aviso(texto = "Tu sesión venció. Ingresá de nuevo para seguir usando la app.")
        }
        TituloSeccion(texto = "Ingresar")
        Descripcion(texto = "Ingresá con tu cuenta de gub.uy para ver tu viaje.")

        when (val actual = estado) {
            is UiState.Loading -> CircularProgressIndicator()
            else -> {
                SoloDebug {
                    OutlinedTextField(
                        value = cedulaPrueba,
                        onValueChange = { cedulaPrueba = it },
                        label = { Text("Cédula (login simulado)") },
                        placeholder = { Text("55555555") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.urlLogin(cedulaPrueba)?.let { url -> abrirEnNavegador(context, url) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ingresar con gub.uy")
                }
                if (actual is UiState.Error) {
                    TextoError(texto = actual.mensaje)
                }
            }
        }
    }
}

// custom tab: un navegador dentro de la app. al terminar el login, el central redirige a
// cargauy://ingreso?code=... y android se lo entrega a MainActivity (intent-filter del manifest)
private fun abrirEnNavegador(context: Context, url: String) {
    CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
}
