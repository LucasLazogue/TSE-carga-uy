package uy.tse.cargauy

import android.app.Application

import uy.tse.cargauy.data.local.CargaUYDatabase

class CargaUYApp : Application() {

    // Una sola instancia de la base para toda la app. Room crea el archivo en el primer acceso.
    val database: CargaUYDatabase by lazy { CargaUYDatabase.crear(this) }
}
