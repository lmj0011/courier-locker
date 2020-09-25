package name.lmj0011.courierlocker.database

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CustomerDao: BaseDao {

    @Insert
    fun insert(customer: Customer)

    @Update
    fun update(customer: Customer)

    @Query("SELECT * from customers_table WHERE id = :key")
    fun get(key: Long): Customer?

    @Query("DELETE FROM customers_table")
    fun clear()

    @Query("SELECT * FROM customers_table ORDER BY id DESC")
    fun getAllCustomersByThePage(): DataSource.Factory<Int, Customer>

    @Query("SELECT * FROM customers_table WHERE name LIKE :query OR address LIKE :query OR note LIKE :query ORDER BY id DESC")
    fun getAllCustomersByThePageFiltered(query: String): DataSource.Factory<Int, Customer>

    @Query("SELECT * FROM customers_table ORDER BY id DESC")
    fun getAllCustomers(): LiveData<MutableList<Customer>>

    @Query("DELETE from customers_table WHERE id = :key")
    fun deleteByCustomerId(key: Long): Int

}