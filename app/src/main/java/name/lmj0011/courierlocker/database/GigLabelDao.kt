package name.lmj0011.courierlocker.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface GigLabelDao: BaseDao {

    @Insert
    fun insert(gigLabel: GigLabel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(gigLabel: MutableList<GigLabel>)

    @Update
    fun update(gigLabel: GigLabel)

    @Update
    fun updateAll(gigLabels: MutableList<GigLabel>)

    @Query("SELECT * from gig_labels_table WHERE id = :key")
    fun get(key: Long): GigLabel?

    @Query("SELECT * FROM `gig_labels_table` ORDER BY `order` ASC")
    fun getAllGigs(): LiveData<MutableList<GigLabel>>

    @Query("SELECT * FROM `gig_labels_table` ORDER BY `order` ASC")
    fun getAllGigsByOrder(): MutableList<GigLabel>

    @Query("SELECT * FROM `gig_labels_table`")
    fun getAllGigsBlocking(): MutableList<GigLabel>

    @Query("SELECT MAX(`order`) FROM `gig_labels_table`")
    fun getMaxOrder(): Int

    @Query("DELETE from gig_labels_table WHERE id = :key")
    fun deleteByGigId(key: Long): Int

    @Query("DELETE FROM gig_labels_table")
    fun clear()


    @Query("SELECT * FROM trips_table")
    fun getAllTrips(): MutableList<Trip>

    @Update
    fun updateAllTrips(trips: MutableList<Trip>)
}