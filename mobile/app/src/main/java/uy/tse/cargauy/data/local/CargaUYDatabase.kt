package uy.tse.cargauy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// version 1 del esquema. cambiar una entidad (tabla o columna) pide subir la version y agregar una
// Migration con addMigrations: sin ella, la app se cierra al abrir una base de la version anterior.
// el esquema de cada version queda en app/schemas (exportSchema) para escribir la migracion contra el
@Database(entities = [EventoPendiente::class, ViajeLocal::class], version = 1, exportSchema = true)
abstract class CargaUYDatabase : RoomDatabase() {

    abstract fun eventoPendienteDao(): EventoPendienteDao

    abstract fun viajeLocalDao(): ViajeLocalDao

    companion object {
        fun crear(context: Context): CargaUYDatabase =
            Room.databaseBuilder(context, CargaUYDatabase::class.java, "carga-uy.db").build()
    }
}
