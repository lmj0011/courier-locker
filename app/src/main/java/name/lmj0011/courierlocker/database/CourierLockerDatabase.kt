package name.lmj0011.courierlocker.database

import android.content.Context
import android.database.sqlite.SQLiteDatabaseLockedException
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.BuildConfig
import timber.log.Timber
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory

@Database(entities = [GateCode::class, Trip::class, Customer::class, Apartment::class, GigLabel::class], version = 9,  exportSchema = true)
@TypeConverters(DataConverters::class)
abstract class CourierLockerDatabase : RoomDatabase() {

    abstract val baseDao: BaseDao
    abstract val gateCodeDao: GateCodeDao
    abstract val tripDao: TripDao
    abstract val customerDao: CustomerDao
    abstract val apartmentDao: ApartmentDao
    abstract val gigLabelDao: GigLabelDao
    abstract val settingsDao: SettingsDao

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
                database.execSQL("ALTER TABLE `trips_table` ADD COLUMN `stops` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `trips_table` ADD COLUMN `notes` TEXT NOT NULL DEFAULT ''")

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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // create GigLabels table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `gig_labels_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `order` INTEGER NOT NULL, `visible` INTEGER NOT NULL)
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `apartments_table` ADD COLUMN `gateCodeId` INTEGER NOT NULL DEFAULT 0")
            }

        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `apartments_table` ADD COLUMN `aboveGroundFloorCount` INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE `apartments_table` ADD COLUMN `belowGroundFloorCount` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `apartments_table` ADD COLUMN `floorOneAsBlueprint` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `apartments_table` ADD COLUMN `buildingUnits` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8,9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                /** Drop column isn't supported by SQLite, so emptying the column seems to be the painless method
                 *  ref: https://www.sqlite.org/faq.html#q11 and https://stackoverflow.com/a/68621973/2445763
                 */
                with(database) {
                    execSQL("UPDATE apartments_table SET uid = ''")
                    execSQL("UPDATE apartments_table SET mapImageUrl = ''")
                    execSQL("UPDATE apartments_table SET sourceUrl = ''")
                }
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
                    val builder = Room.databaseBuilder(
                        context.applicationContext,
                        CourierLockerDatabase::class.java,
                        "courier_locker_database"
                    )
                    .addMigrations(
                        /**
                         * check AppDataImportExportHelper after adding new a Migration
                         * it may need updating to reflect DB change
                         */
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                        MIGRATION_7_8, MIGRATION_8_9
                    )

                    if (BuildConfig.DEBUG) {
                        builder
                            .fallbackToDestructiveMigration()
                    }

                    instance = builder
                        .openHelperFactory(RequerySQLiteOpenHelperFactory())
                        .build()
                }

                return instance
            }
        }

        /**
         * Blocks app until Database is not locked or in a transaction.
         * Should only need to be called once when running a large migration
         */
        fun blockUntilDbIsAccessible(context: Context) {
            Timber.d("Accessing Database...")
            val dbStartTime = System.currentTimeMillis()
            runBlocking {
                val database = getInstance(context)
                var isLocked = true

                while(isLocked) {
                    try {
                        isLocked = database.inTransaction()
                    } catch(ex: SQLiteDatabaseLockedException) {
                        delay(500)
                    }
                }
            }
            val dbEndTime = System.currentTimeMillis()
            Timber.d("Accessing Database took ${dbEndTime - dbStartTime}ms")
        }

    }

}