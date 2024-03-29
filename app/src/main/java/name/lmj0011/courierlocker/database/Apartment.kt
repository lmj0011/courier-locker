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

    @Deprecated("No longer used.")
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

    @ColumnInfo(name = "aboveGroundFloorCount")
    var aboveGroundFloorCount: Int = 1,

    @ColumnInfo(name = "belowGroundFloorCount")
    var belowGroundFloorCount: Int = 0,

    @ColumnInfo(name = "floorOneAsBlueprint")
    var floorOneAsBlueprint: Boolean = true,

    @Deprecated("No longer used.")
    @ColumnInfo(name = "mapImageUrl")
    var mapImageUrl: String = "",

    @Deprecated("No longer used.")
    @ColumnInfo(name = "sourceUrl")
    var sourceUrl: String = "",

    @ColumnInfo(name = "buildings")
    var buildings: MutableList<Building> = mutableListOf(),

    @ColumnInfo(name = "buildingUnits")
    var buildingUnits: MutableList<BuildingUnit> = mutableListOf()
): Addressable
