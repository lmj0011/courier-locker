package name.lmj0011.courierlocker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [GateCode::class], version = 1,  exportSchema = true)
@TypeConverters(DataConverters::class)
abstract class CourierLockerDatabase : RoomDatabase() {

    abstract val gateCodesDao: GateCodesDao

    companion object {

        @Volatile
        private var INSTANCE: CourierLockerDatabase? = null

        fun getInstance(context: Context): CourierLockerDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        CourierLockerDatabase::class.java,
                        "courier_locker_database"
                    )
                        // Wipes and rebuilds instead of migrating if no Migration object.
                        // Migration is not part of this lesson. You can learn more about
                        // migration with Room in this blog post:
                        // https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929
                        .fallbackToDestructiveMigration()
                        .build()
                }

                return instance
            }
        }
    }

}