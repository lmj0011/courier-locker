package name.lmj0011.courierlocker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

@Database(entities = [GateCode::class, Trip::class, Customer::class, Apartment::class], version = 5,  exportSchema = true)
@TypeConverters(DataConverters::class)
abstract class CourierLockerDatabase : RoomDatabase() {

    abstract val baseDao: BaseDao
    abstract val gateCodeDao: GateCodeDao
    abstract val tripDao: TripDao
    abstract val customerDao: CustomerDao
    abstract val apartmentDao: ApartmentDao

    companion object {
        // Room already created the GateCode table based on it's data class in DB version 1: https://developer.android.com/training/data-storage/room/defining-data

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // create Trips table
                database.execSQL("CREATE TABLE IF NOT EXISTS `trips_table`" +
                        " (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` TEXT NOT NULL, "+
                        "`pickupAddress` TEXT NOT NULL, `pickupAddressLatitude` REAL NOT NULL, `pickupAddressLongitude` REAL NOT NULL,"+
                        "`dropOffAddress` TEXT NOT NULL, `dropOffAddressLatitude` REAL NOT NULL, `dropOffAddressLongitude` REAL NOT NULL,"+
                        "`distance` REAL NOT NULL, `payAmount` TEXT NOT NULL, `gigName` TEXT NOT NULL )")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // create Customers Table
                database.execSQL("CREATE TABLE IF NOT EXISTS `customers_table`" +
                        " (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"+
                        "`name` TEXT NOT NULL, `address` TEXT NOT NULL, `addressLatitude` REAL NOT NULL,"+
                        "`addressLongitude` REAL NOT NULL, `impression` INTEGER NOT NULL, `note` TEXT NOT NULL)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            // alter Trips table; add stops and notes columns
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `trips_table` ADD COLUMN `stops` TEXT NOT NULL DEFAULT ''");
                database.execSQL("ALTER TABLE `trips_table` ADD COLUMN `notes` TEXT NOT NULL DEFAULT ''");

            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // create Apartments table
                database.execSQL("CREATE TABLE IF NOT EXISTS `apartments_table`" +
                        " (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uid` TEXT NOT NULL, "+
                        "`name` TEXT NOT NULL, `address` TEXT NOT NULL, `latitude` REAL NOT NULL,"+
                        "`longitude` REAL NOT NULL, `mapImageUrl` TEXT NOT NULL, `sourceUrl` TEXT NOT NULL,"+
                        "`buildings` TEXT NOT NULL )")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                }

                return instance
            }
        }

        fun getDbData(context: Context): ByteArray? {
            var data: ByteArray? = null

            try {
                val currentdb = context.getDatabasePath("courier_locker_database")

                data = currentdb.readBytes()
            } catch (e: Exception) {
                Timber.i("backup db failed!")
            }

            return data
        }

        fun setDbData(context: Context, data: ByteArray) {
            try {
                val currentdb = context.getDatabasePath("courier_locker_database")
                val currentdbSHM = context.getDatabasePath("courier_locker_database-shm")
                val currentdbWAL = context.getDatabasePath("courier_locker_database-wal")
                val output = currentdb.outputStream()

                output.write(data)
                output.flush()
                output.close()

                if(currentdbSHM.delete() && currentdbWAL.delete()) {
                    Timber.i("journal files (-shm -wal) deleted successfully!")
                }

                Timber.i("imported db successfully!")
            } catch (e: Exception) {
                Timber.i("importing db failed!")
            }

        }
    }

}