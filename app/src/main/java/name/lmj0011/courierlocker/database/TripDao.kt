package name.lmj0011.courierlocker.database

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

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

    @RawQuery(observedEntities = [Trip::class])
    fun getAllTripsByThePageFiltered(query: SupportSQLiteQuery): DataSource.Factory<Int, Trip>

    @Query("SELECT * FROM trips_table ORDER BY id DESC")
    fun getAllTrips(): LiveData<MutableList<Trip>>

    @Query("SELECT * FROM trips_table WHERE strftime('%Y-%m-%d',date(timestamp, 'localtime')) = strftime('%Y-%m-%d',date('now', 'localtime'))")
    fun getAllTodayTrips(): List<Trip>

    @Query("SELECT * FROM trips_table WHERE distance <= 0.0")
    fun getAllNoDistanceTrips(): List<Trip>

    @Query("SELECT payAmount FROM trips_table")
    fun getAllTripPayAmounts(): List<String>

    @Query("SELECT payAmount FROM trips_table WHERE date(timestamp, 'localtime') >= date('now', 'localtime') AND date(timestamp, 'localtime') < date('now', 'localtime', '+1 day')")
    fun getAllTodayTripPayAmounts(): List<String>

    @Query("SELECT payAmount FROM trips_table WHERE strftime('%Y-%m',date(timestamp, 'localtime')) = strftime('%Y-%m',date('now', 'localtime'))")
    fun getAllMonthTripPayAmounts(): List<String>

    @Query("SELECT * FROM trips_table WHERE date(timestamp, 'localtime') >= date(:startDate, 'unixepoch') AND date(timestamp, 'localtime') <= date(:endDate, 'unixepoch')")
    fun getAllTripsInDateRange(startDate: Long, endDate: Long): List<Trip>

    @Query("DELETE from trips_table WHERE id = :key")
    fun deleteByTripId(key: Long): Int

    @Query("SELECT * FROM `gig_labels_table` WHERE visible = 1 ORDER BY `order` ASC")
    fun getAllGigsThatAreVisible(): MutableList<GigLabel>

}