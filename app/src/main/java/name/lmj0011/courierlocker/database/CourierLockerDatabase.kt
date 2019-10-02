package name.lmj0011.courierlocker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [GateCode::class, Trip::class, Customer::class], version = 3,  exportSchema = true)
@TypeConverters(DataConverters::class)
abstract class CourierLockerDatabase : RoomDatabase() {

    abstract val gateCodeDao: GateCodeDao
    abstract val tripDao: TripDao
    abstract val customerDao: CustomerDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `trips_table`" +
                        " (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` TEXT NOT NULL, "+
                        "`pickupAddress` TEXT NOT NULL, `pickupAddressLatitude` REAL NOT NULL, `pickupAddressLongitude` REAL NOT NULL,"+
                        "`dropOffAddress` TEXT NOT NULL, `dropOffAddressLatitude` REAL NOT NULL, `dropOffAddressLongitude` REAL NOT NULL,"+
                        "`distance` REAL NOT NULL, `payAmount` TEXT NOT NULL, `gigName` TEXT NOT NULL )")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `customers_table`" +
                        " (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"+
                        "`name` TEXT NOT NULL, `address` TEXT NOT NULL, `addressLatitude` REAL NOT NULL,"+
                        "`addressLongitude` REAL NOT NULL, `impression` INTEGER NOT NULL, `note` TEXT NOT NULL)")
            }
        }

        @Volatile
        private var INSTANCE: CourierLockerDatabase? = null

        /**
         * about migrations: https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929
         *
         */
        fun getInstance(context: Context): CourierLockerDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        CourierLockerDatabase::class.java,
                        "courier_locker_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                }

                return instance
            }
        }
    }

}