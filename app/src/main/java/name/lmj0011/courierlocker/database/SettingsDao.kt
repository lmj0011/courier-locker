package name.lmj0011.courierlocker.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SettingsDao: BaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertApartments(apartments: MutableList<Apartment>)

    @Query("SELECT * FROM apartments_table ORDER BY id DESC")
    fun getAllApartments(): List<Apartment>

    @Query("DELETE FROM apartments_table")
    fun clearApartments()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrips(trips: MutableList<Trip>)

    @Query("SELECT * FROM trips_table ORDER BY id DESC")
    fun getAllTrips(): List<Trip>

    @Query("DELETE FROM trips_table")
    fun clearTrips()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGateCodes(gatecodes: MutableList<GateCode>)

    @Query("SELECT * FROM gate_codes_table ORDER BY id DESC")
    fun getAllGateCodes(): List<GateCode>

    @Query("DELETE FROM gate_codes_table")
    fun clearGateCodes()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCustomers(customers: MutableList<Customer>)

    @Query("SELECT * FROM customers_table ORDER BY id DESC")
    fun getAllCustomers(): List<Customer>

    @Query("DELETE FROM customers_table")
    fun clearCustomers()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGigLabels(gigLabel: MutableList<GigLabel>)

    @Query("SELECT * FROM `gig_labels_table` ORDER BY `order` ASC")
    fun getAllGigs(): MutableList<GigLabel>

    @Query("DELETE FROM gig_labels_table")
    fun clearGigLabels()

}