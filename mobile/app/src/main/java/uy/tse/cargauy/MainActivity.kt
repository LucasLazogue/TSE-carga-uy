package uy.tse.cargauy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

import uy.tse.cargauy.data.PreferenciasRepository
import uy.tse.cargauy.ui.navigation.NavGraph
import uy.tse.cargauy.ui.theme.CargaUYTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencias = PreferenciasRepository(applicationContext)

        setContent {
            val temaOscuro by preferencias.temaOscuro.collectAsStateWithLifecycle(initialValue = false)
            val urlServidor by preferencias.urlServidor.collectAsStateWithLifecycle(initialValue = "")
            val scope = rememberCoroutineScope()

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
                    }
                )
            }
        }
    }
}