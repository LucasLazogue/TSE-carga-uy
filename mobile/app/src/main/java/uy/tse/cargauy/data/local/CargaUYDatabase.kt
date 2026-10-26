package uy.tse.cargauy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// version 3 del esquema. cambiar una entidad (tabla o columna) pide subir la version y agregar una
// Migration con addMigrations: sin ella, la app se cierra al abrir una base de la version anterior.
// el esquema de cada version queda en app/schemas (exportSchema) para escribir la migracion contra el
@Database(entities = [EventoPendiente::class, ViajeLocal::class, RefreshToken::class], version = 3, exportSchema = true)
abstract class CargaUYDatabase : RoomDatabase() {

    abstract fun eventoPendienteDao(): EventoPendienteDao

    abstract fun viajeLocalDao(): ViajeLocalDao

    abstract fun refreshTokenDao(): RefreshTokenDao

    companion object {
        // el sql es el createSql de schemas/.../2.json
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `refresh_token` (`idUsuario` INTEGER NOT NULL, `token` TEXT NOT NULL, " +
                        "PRIMARY KEY(`idUsuario`))"
                )
            }
        }

        // columnas nuevas que admiten null: los eventos sin enviar de la version 2 se conservan y se mandan igual
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `viaje_local` ADD COLUMN `fechaInicio` TEXT")
                db.execSQL("ALTER TABLE `evento_pendiente` ADD COLUMN `descripcion` TEXT")
                db.execSQL("ALTER TABLE `evento_pendiente` ADD COLUMN `kilos` INTEGER")
            }
        }

        fun crear(context: Context): CargaUYDatabase =
            Room.databaseBuilder(context, CargaUYDatabase::class.java, "carga-uy.db")
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                .build()
    }
}
