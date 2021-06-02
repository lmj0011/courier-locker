package name.lmj0011.courierlocker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import name.lmj0011.courierlocker.helpers.interfaces.Addressable

@Entity(tableName = "apartments_table")
data class Apartment(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,

    @ColumnInfo(name = "gateCodeId")
    var gateCodeId: Long = 0L,

    // currently only relevant if Apartment was pulled from an external source. (ie. not self created)
    @ColumnInfo(name = "uid")
    var uid: String = "",

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "address")
    override var address: String = "",

    @ColumnInfo(name = "latitude")
    override var latitude: Double = 0.0,

    @ColumnInfo(name = "longitude")
    override var longitude: Double = 0.0,

    // can be omitted
    @ColumnInfo(name = "mapImageUrl")
    var mapImageUrl: String = "",

    // currently only relevant if Apartment was pulled from an external source. (ie. not self created)
    @ColumnInfo(name = "sourceUrl")
    var sourceUrl: String = "",

    @ColumnInfo(name = "buildings")
    var buildings: MutableList<Building> = mutableListOf()
): Addressable
