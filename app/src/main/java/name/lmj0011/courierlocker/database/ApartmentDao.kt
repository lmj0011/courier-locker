package name.lmj0011.courierlocker.database

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*

@Dao
interface ApartmentDao: BaseDao {

    @Insert
    fun insert(apartment: Apartment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(apartments: MutableList<Apartment>)

    @Update
    fun update(apartment: Apartment)

    @Query("SELECT * from apartments_table WHERE id = :key")
    fun get(key: Long): Apartment?

    @Query("SELECT * from gate_codes_table WHERE id = :gateCodeId")
    fun getRelatedGateCode(gateCodeId: Long): GateCode?

    @Query("DELETE FROM apartments_table")
    fun clear()

    @Query("SELECT * FROM apartments_table WHERE gateCodeId = 0 ORDER BY id DESC")
    fun getAllApartmentsWithoutGateCodeByThePage(): DataSource.Factory<Int, Apartment>

    @Query("SELECT * FROM apartments_table ORDER BY id DESC")
    fun getAllApartmentsByThePage(): DataSource.Factory<Int, Apartment>

    @Query("SELECT * FROM apartments_table WHERE name LIKE :query OR address LIKE :query ORDER BY id DESC")
    fun getAllApartmentsByThePageFiltered(query: String): DataSource.Factory<Int, Apartment>

    @Query("SELECT * FROM apartments_table ORDER BY id DESC")
    fun getAllApartments(): LiveData<MutableList<Apartment>>

    @Query("DELETE from apartments_table WHERE id = :key")
    fun deleteByApartmentId(key: Long): Int

    @Delete
    fun deleteAll(apartments: MutableList<Apartment>)
}