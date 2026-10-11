package uy.tse.cargauy.data

import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import retrofit2.HttpException

import uy.tse.cargauy.CargaUYApp
import uy.tse.cargauy.data.local.EventoPendiente
import uy.tse.cargauy.data.local.ViajeLocal
import uy.tse.cargauy.data.remote.ApiClient
import uy.tse.cargauy.data.remote.MovilApi
import uy.tse.cargauy.sync.Sincronizacion

/**
 * El único lugar que decide entre la red y Room; las pantallas y el worker no conocen Retrofit.
 * Todo es del usuario logueado: en un celular compartido, los eventos de otro chofer quedan guardados
 * sin mostrarse y se envían cuando ese chofer vuelva a ingresar.
 */
@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
class ViajeRepository(private val app: CargaUYApp) {

    private val viajeDao = app.database.viajeLocalDao()
    private val eventoDao = app.database.eventoPendienteDao()

    // flatMapLatest: cuando cambia el usuario logueado, se deja de mirar lo del anterior y se mira lo del nuevo
    val viaje: Flow<ViajeLocal?> = app.sesion.idUsuario.flatMapLatest { id ->
        if (id == null) flowOf(null) else viajeDao.observar(id)
    }
    val pendientes: Flow<Int> = app.sesion.idUsuario.flatMapLatest { id ->
        if (id == null) flowOf(0) else eventoDao.contar(id)
    }
    val enviados: Flow<List<EventoPendiente>> = app.sesion.idUsuario.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else eventoDao.observarEnviados(id)
    }

    private suspend fun usuarioActual(): Long? = app.sesion.idUsuario.first()

    private suspend fun movilApi(): MovilApi =
        ApiClient.crear(app.preferencias.urlServidor.first(), app.sesion).create(MovilApi::class.java)

    // trae el viaje del central. sin red tira IOException y se sigue con lo guardado en Room.
    // si hay eventos sin enviar no se descarga: el central todavia no los tiene y pisaria el estado local;
    // se envian primero y la respuesta del envio ya trae el viaje actualizado
    suspend fun actualizar() {
        val idUsuario = usuarioActual() ?: return
        if (eventoDao.pendientes(idUsuario).isNotEmpty()) {
            Sincronizacion.programar(app)
            return
        }
        val respuesta = movilApi().viaje()
        when {
            respuesta.code() == 204 -> viajeDao.borrar(idUsuario) // no tiene viajes asignados
            respuesta.isSuccessful -> respuesta.body()?.let { viajeDao.reemplazar(aViajeLocal(it, idUsuario)) }
            else -> throw HttpException(respuesta)
        }
    }

    // con o sin señal el camino es el mismo: Room y despues el worker. asi el modo sin conexion
    // no es un caso aparte que se pueda romper
    suspend fun registrarEvento(tipo: TipoEvento) {
        val idUsuario = usuarioActual() ?: return
        val viaje = viajeDao.actual(idUsuario) ?: return
        eventoDao.insertar(
            EventoPendiente(
                idEvento = UUID.randomUUID().toString(),
                idUsuario = idUsuario,
                idViaje = viaje.idViaje,
                tipo = tipo.name,
                // la hora en que el chofer toco el boton, no la del envio
                timestampGeneracion = System.currentTimeMillis()
            )
        )
        estadoTras(viaje.estado, tipo)?.let { viajeDao.cambiarEstado(idUsuario, it) }
        Sincronizacion.programar(app)
    }

    // manda al central los eventos del usuario logueado (con su token) y guarda lo que respondio por cada uno.
    // los de otro chofer no se tocan: con este token el central los rechazaria y se perderian. la llama el worker
    suspend fun sincronizar() {
        val idUsuario = usuarioActual() ?: return
        val pendientes = eventoDao.pendientes(idUsuario)
        if (pendientes.isEmpty()) return
        val respuesta = movilApi().sync(pendientes.map { aEventoDto(it) })
        for (resultado in respuesta.resultados) {
            eventoDao.marcar(resultado.idEvento, resultado.resultado, resultado.mensaje)
        }
        // lo que decidio el central pisa lo local, salvo que se hayan registrado eventos nuevos mientras se enviaba
        if (eventoDao.pendientes(idUsuario).isEmpty()) {
            val viaje = respuesta.viaje
            if (viaje == null) viajeDao.borrar(idUsuario) else viajeDao.reemplazar(aViajeLocal(viaje, idUsuario))
        }
    }

    suspend fun borrarEnviados() {
        val idUsuario = usuarioActual() ?: return
        eventoDao.borrarEnviados(idUsuario)
    }
}
