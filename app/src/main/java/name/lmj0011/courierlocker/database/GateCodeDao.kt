package name.lmj0011.courierlocker.database

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*

@Dao
interface GateCodeDao: BaseDao {

    @Insert
    fun insert(gateCode: GateCode)

    @Update
    fun update(gateCode: GateCode)

    @Update
    fun update(apartment: Apartment)

    @Query("SELECT * from gate_codes_table WHERE id = :key")
    fun get(key: Long): GateCode?

    @Query("SELECT * from apartments_table WHERE gateCodeId = :gateCodeId")
    fun getRelatedApartment(gateCodeId: Long): Apartment?

    @Transaction
    @Query("SELECT * from gate_codes_table WHERE id = :key")
    fun getGateCodeAndApartment(key: Long): GateCodeAndApartment?

    @Query("DELETE FROM gate_codes_table")
    fun clear()

    @Transaction
    @Query("SELECT * FROM gate_codes_table ORDER BY id DESC")
    fun getAllGateCodeAndApartmentsByThePage(): DataSource.Factory<Int, GateCodeAndApartment>

    @Query("SELECT * FROM gate_codes_table ORDER BY id DESC")
    fun getAllGateCodesByThePage(): DataSource.Factory<Int, GateCode>

    @Query("SELECT * FROM gate_codes_table WHERE address LIKE :query OR codes LIKE :query ORDER BY id DESC")
    fun getAllGateCodesByThePageFiltered(query: String): DataSource.Factory<Int, GateCode>

    @Query("SELECT * FROM gate_codes_table ORDER BY id DESC")
    fun getAllGateCodes(): LiveData<MutableList<GateCode>>

    @Query("DELETE from gate_codes_table WHERE id = :key")
    fun deleteByGateCodeId(key: Long): Int

}