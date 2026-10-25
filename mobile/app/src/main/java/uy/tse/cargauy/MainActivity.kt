package uy.tse.cargauy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

import uy.tse.cargauy.data.ViajeRepository
import uy.tse.cargauy.data.auth.verificarSesionConCentral
import uy.tse.cargauy.data.remote.hayRed
import uy.tse.cargauy.ui.navigation.NavGraph
import uy.tse.cargauy.ui.theme.CargaUYTheme

class MainActivity : ComponentActivity() {

    // la ultima vuelta del login (cargauy://ingreso?...), hasta que la pantalla de login la procese
    private val ingreso = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // la app estaba cerrada y la abrio el deep link. solo la primera vez: al rotar la pantalla,
        // onCreate corre de nuevo con el mismo intent y ese code ya se uso
        if (savedInstanceState == null) recibir(intent)

        val app = application as CargaUYApp
        val preferencias = app.preferencias
        val sesion = app.sesion

        setContent {
            val temaOscuro by preferencias.temaOscuro.collectAsStateWithLifecycle(initialValue = false)
            val urlServidor by preferencias.urlServidor.collectAsStateWithLifecycle(initialValue = "")
            val haySesion by sesion.haySesion.collectAsStateWithLifecycle<Boolean?>(initialValue = null)
            val sesionVencida by sesion.vencida.collectAsStateWithLifecycle(initialValue = false)
            val uriIngreso by ingreso.collectAsStateWithLifecycle()
            val conRed by remember { hayRed(applicationContext) }.collectAsStateWithLifecycle(initialValue = false)
            val scope = rememberCoroutineScope()

            // con la sesion vencida, apenas hay red (o al volver a la app) se consulta al central:
            // si responde 401, la sesion se cierra y aparece el login sin que el chofer haga nada
            LaunchedEffect(haySesion, sesionVencida, conRed) {
                if (haySesion == true && sesionVencida && conRed) verificarSesionConCentral(app)
            }

            CargaUYTheme(darkTheme = temaOscuro) {
                NavGraph(
                    temaOscuro = temaOscuro,
                    onCambiarTema = { nuevo ->
                        scope.launch { preferencias.guardarTemaOscuro(nuevo) }
                    },
                    urlServidor = urlServidor,
                    onGuardarUrl = { nueva ->
                        scope.launch { preferencias.guardarUrlServidor(nueva) }
                    },
                    onRestaurarUrl = {
                        scope.launch { preferencias.restaurarUrlServidor() }
                    },
                    haySesion = haySesion,
                    sesionVencida = sesionVencida,
                    ingreso = uriIngreso,
                    onIngresoProcesado = { ingreso.value = null },
                    onCerrarSesion = {
                        scope.launch { ViajeRepository(app).cerrarSesion() }
                    }
                )
            }
        }
    }

    // la app ya estaba abierta: android entrega el deep link a esta misma pantalla en vez de abrir otra
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        recibir(intent)
    }

    private fun recibir(intent: Intent?) {
        val uri = intent?.data
        if (uri?.scheme == "cargauy") ingreso.value = uri
    }
}
