package name.lmj0011.courierlocker.database

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
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
    fun getAllTripsByThePage(): DataSource.Factory<Int, Trip>

    @Query("SELECT * FROM trips_table WHERE pickupAddress LIKE :query OR dropOffAddress LIKE :query OR payAmount LIKE :query OR gigName LIKE :query ORDER BY id DESC")
    fun getAllTripsByThePageFiltered(query: String): DataSource.Factory<Int, Trip>

    @Query("SELECT * FROM trips_table ORDER BY id DESC")
    fun getAllTrips(): LiveData<MutableList<Trip>>

    @Query("SELECT * FROM trips_table WHERE distance <= 0.0")
    fun getAllNoDistanceTrips(): List<Trip>

    @Query("SELECT payAmount FROM trips_table")
    fun getAllTripPayAmounts(): LiveData<List<String>>

    @Query("SELECT payAmount FROM trips_table WHERE date(timestamp, 'localtime') >= date('now', 'localtime') AND date(timestamp, 'localtime') < date('now', 'localtime', '+1 day')")
    fun getAllTodayTripPayAmounts(): LiveData<List<String>>

    @Query("SELECT payAmount FROM trips_table WHERE strftime('%Y',date(timestamp, 'localtime')) = strftime('%Y',date('now', 'localtime')) AND  strftime('%m',date(timestamp, 'localtime')) = strftime('%m',date('now', 'localtime'))")
    fun getAllMonthTripPayAmounts(): LiveData<List<String>>

    @Query("DELETE from trips_table WHERE id = :key")
    fun deleteByTripId(key: Long): Int

    @Query("SELECT * FROM `gig_labels_table` WHERE visible = 1 ORDER BY `order` ASC")
    fun getAllGigsThatAreVisible(): MutableList<GigLabel>

}