package name.lmj0011.courierlocker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import name.lmj0011.courierlocker.helpers.interfaces.Addressable

@Entity(tableName = "gate_codes_table")
data class GateCode(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,

    @ColumnInfo(name = "address")
    override var address: String = "",

    @ColumnInfo(name = "latitude")
    override var latitude: Double = 0.toDouble(),

    @ColumnInfo(name = "longitude")
    override var longitude: Double = 0.toDouble(),

    @ColumnInfo(name = "codes")
    var codes: MutableList<String> = mutableListOf()
) : Addressable

