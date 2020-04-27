package name.lmj0011.courierlocker.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TripDao: BaseDao {

    @Insert
    fun insert(trip: Trip)

    @Update
    fun update(trip: Trip)

    @Query("SELECT * from trips_table WHERE id = :key")
    fun get(key: Long): Trip?

    @Query("DELETE FROM trips_table")
    fun clear()

    @Query("SELECT * FROM trips_table ORDER BY id DESC")
    fun getAllTrips(): LiveData<MutableList<Trip>>

    @Query("DELETE from trips_table WHERE id = :key")
    fun deleteByTripId(key: Long): Int

}