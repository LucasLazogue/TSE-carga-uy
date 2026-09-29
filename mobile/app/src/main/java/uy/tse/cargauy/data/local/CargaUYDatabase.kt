package uy.tse.cargauy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EventoPendiente::class], version = 1, exportSchema = false)
abstract class CargaUYDatabase : RoomDatabase() {

    abstract fun eventoPendienteDao(): EventoPendienteDao

    companion object {
        fun crear(context: Context): CargaUYDatabase =
            Room.databaseBuilder(context, CargaUYDatabase::class.java, "carga-uy.db").build()
    }
}
