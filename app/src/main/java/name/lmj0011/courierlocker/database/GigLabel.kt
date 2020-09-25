package name.lmj0011.courierlocker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gig_labels_table")
data class GigLabel (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "order")
    var order: Int = 0,

    @ColumnInfo(name = "visible")
    var visible: Boolean = true
)