package name.lmj0011.courierlocker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "gate_codes_table")
data class GateCode(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,

    @ColumnInfo(name = "address")
    var address: String = "",

    @ColumnInfo(name = "codes")
    var codes: MutableList<String> = mutableListOf()
)

