package uy.tse.cargauy.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

// true mientras el celular tiene una red (wifi o datos), false sin ninguna (por ejemplo, en modo avion).
fun hayRed(context: Context): Flow<Boolean> = callbackFlow {
    val conectividad = context.getSystemService(ConnectivityManager::class.java)
    val avisos = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(true)
        }

        override fun onLost(network: Network) {
            trySend(false)
        }
    }
    // android solo avisa cuando aparece una red: si arranca sin ninguna, el valor inicial sale de aca
    trySend(conectividad.activeNetwork != null)
    conectividad.registerDefaultNetworkCallback(avisos)
    awaitClose { conectividad.unregisterNetworkCallback(avisos) }
}.distinctUntilChanged()
