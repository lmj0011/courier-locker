package name.lmj0011.courierlocker.database

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GateCodeDao: BaseDao {

    @Insert
    fun insert(gateCode: GateCode)

    @Update
    fun update(gateCode: GateCode)

    @Query("SELECT * from gate_codes_table WHERE id = :key")
    fun get(key: Long): GateCode?

    @Query("DELETE FROM gate_codes_table")
    fun clear()

    @Query("SELECT * FROM gate_codes_table ORDER BY id DESC")
    fun getAllGateCodesByThePage(): DataSource.Factory<Int, GateCode>

    @Query("SELECT * FROM gate_codes_table WHERE address LIKE :query OR codes LIKE :query ORDER BY id DESC")
    fun getAllGateCodesByThePageFiltered(query: String): DataSource.Factory<Int, GateCode>

    @Query("SELECT * FROM gate_codes_table ORDER BY id DESC")
    fun getAllGateCodes(): LiveData<MutableList<GateCode>>

    @Query("DELETE from gate_codes_table WHERE id = :key")
    fun deleteByGateCodeId(key: Long): Int

}