package uy.tse.cargauy.ui.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

import uy.tse.cargauy.data.auth.UsuarioDelToken
import uy.tse.cargauy.data.cedulaLegible
import uy.tse.cargauy.ui.components.BotonSecundario
import uy.tse.cargauy.ui.components.DatoFila
import uy.tse.cargauy.ui.components.Descripcion
import uy.tse.cargauy.ui.components.Seccion
import uy.tse.cargauy.ui.components.SoloDebug
import uy.tse.cargauy.ui.components.Tarjeta
import uy.tse.cargauy.ui.components.TituloSeccion
import uy.tse.cargauy.ui.theme.Spacing

@Composable
fun SettingsScreen(
    temaOscuro: Boolean,
    onCambiarTema: (Boolean) -> Unit,
    urlServidor: String,
    onGuardarUrl: (String) -> Unit,
    onRestaurarUrl: () -> Unit,
    // null sin sesion (por ejemplo, desde el login)
    usuario: UsuarioDelToken?,
    onCerrarSesion: (() -> Unit)?
) {
    Column(
        modifier = Modifier.padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        if (usuario != null && onCerrarSesion != null) {
            SeccionCuenta(usuario = usuario, onCerrarSesion = onCerrarSesion)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                TituloSeccion(texto = "Tema oscuro")
                Descripcion(texto = "Cambia los colores de la aplicación")
            }
            Switch(
                checked = temaOscuro,
                onCheckedChange = onCambiarTema
            )
        }

        // en release la url es fija (la de produccion), asi que no tiene sentido mostrarla
        SoloDebug {
            SeccionServidor(
                urlServidor = urlServidor,
                onGuardarUrl = onGuardarUrl,
                onRestaurarUrl = onRestaurarUrl
            )
        }
    }
}

@Composable
private fun SeccionCuenta(usuario: UsuarioDelToken, onCerrarSesion: () -> Unit) {
    Seccion(titulo = "Cuenta") {
        Tarjeta {
            usuario.cedula?.let { DatoFila(etiqueta = "Cédula", valor = cedulaLegible(it)) }
            usuario.correo?.let { DatoFila(etiqueta = "Correo", valor = it) }
        }
        BotonSecundario(texto = "Cerrar sesión", onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SeccionServidor(
    urlServidor: String,
    onGuardarUrl: (String) -> Unit,
    onRestaurarUrl: () -> Unit
) {
    // lo que se esta escribiendo; vuelve al valor guardado cuando este cambia (por ejemplo, al restaurar)
    var url by remember(urlServidor) { mutableStateOf(urlServidor) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Column {
            TituloSeccion(texto = "Servidor")
            Descripcion(texto = "Solo en la versión de desarrollo. Restaurar vuelve a la URL de local.properties.")
        }
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL de la API") },
            placeholder = { Text("http://192.168.1.50:8080/carga-uy/api/") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onRestaurarUrl) {
                Text("Restaurar")
            }
            Button(
                onClick = { onGuardarUrl(url) },
                enabled = url.isNotBlank() && url != urlServidor
            ) {
                Text("Guardar")
            }
        }
    }
}
