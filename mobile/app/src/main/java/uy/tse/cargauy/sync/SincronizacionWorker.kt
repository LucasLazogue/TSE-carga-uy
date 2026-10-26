package uy.tse.cargauy.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.IOException
import retrofit2.HttpException

import uy.tse.cargauy.CargaUYApp
import uy.tse.cargauy.data.ViajeRepository
import uy.tse.cargauy.observabilidad.Telemetria

/** Envía al central los eventos guardados en Room. Lo corre WorkManager cuando hay red, aunque la app esté cerrada. */
class SincronizacionWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CargaUYApp
        return try {
            ViajeRepository(app).sincronizar()
            Result.success()
        } catch (e: HttpException) {
            // 401: ApiClient ya cerro la sesion y la app pide el login; al ingresar se programa de nuevo.
            // 5xx: fallo el servidor y se reintenta mas tarde. otro 4xx: reintentar no lo arregla
            if (e.code() >= 500) Result.retry() else Result.failure()
        } catch (e: IOException) {
            // hay red pero el central no responde: WorkManager reintenta con una espera cada vez mas larga
            Result.retry()
        } finally {
            // la app puede estar cerrada y el proceso terminar antes del proximo lote de spans
            Telemetria.enviarPendientes()
        }
    }
}
