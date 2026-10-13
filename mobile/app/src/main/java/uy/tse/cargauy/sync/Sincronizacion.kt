package uy.tse.cargauy.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Sincronizacion {

    // se llama despues de registrar un evento, al ingresar y al abrir la pantalla con eventos sin enviar
    fun programar(contexto: Context) {
        val pedido = OneTimeWorkRequestBuilder<SincronizacionWorker>()
            // CONNECTED: WorkManager espera a que haya red, aunque la app este cerrada
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // unico: nunca hay dos envios a la vez mandando la misma cola. APPEND_OR_REPLACE y no KEEP:
        // un evento registrado mientras corre un envio se manda en otra corrida al terminar esta
        WorkManager.getInstance(contexto)
            .enqueueUniqueWork("sincronizacion", ExistingWorkPolicy.APPEND_OR_REPLACE, pedido)
    }
}
