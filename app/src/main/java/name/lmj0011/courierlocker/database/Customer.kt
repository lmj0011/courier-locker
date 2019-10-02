package name.lmj0011.courierlocker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "customers_table")
data class Customer(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "address")
    var address: String = "",

    @ColumnInfo(name = "addressLatitude")
    var addressLatitude: Double = 0.0,

    @ColumnInfo(name = "addressLongitude")
    var addressLongitude: Double = 0.0,

    @ColumnInfo(name = "impression")
    var impression: Int = 0,

    @ColumnInfo(name = "note")
    var note: String = ""
)