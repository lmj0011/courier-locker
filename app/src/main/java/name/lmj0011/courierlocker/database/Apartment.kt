package name.lmj0011.courierlocker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apartments_table")
data class Apartment(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,

    @ColumnInfo(name = "uid")
    var uid: String = "",

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "address")
    var address: String = "",

    @ColumnInfo(name = "latitude")
    var latitude: Double = 0.0,

    @ColumnInfo(name = "longitude")
    var longitude: Double = 0.0,

    @ColumnInfo(name = "mapImageUrl")
    var mapImageUrl: String = "",

    @ColumnInfo(name = "sourceUrl")
    var sourceUrl: String = "",

    @ColumnInfo(name = "buildings")
    var buildings: List<Building> = mutableListOf()
)