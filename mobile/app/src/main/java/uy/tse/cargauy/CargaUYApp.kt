package uy.tse.cargauy

import android.app.Application

import uy.tse.cargauy.data.PreferenciasRepository
import uy.tse.cargauy.data.auth.RefreshTokens
import uy.tse.cargauy.data.auth.SesionRepository
import uy.tse.cargauy.data.local.CargaUYDatabase

class CargaUYApp : Application() {

    // Una sola instancia de la base para toda la app. Room crea el archivo en el primer acceso.
    val database: CargaUYDatabase by lazy { CargaUYDatabase.crear(this) }

    val preferencias: PreferenciasRepository by lazy { PreferenciasRepository(this) }
    val sesion: SesionRepository by lazy { SesionRepository(this) }
    val refreshTokens: RefreshTokens by lazy { RefreshTokens(this) }
}
