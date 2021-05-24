package name.lmj0011.courierlocker.database

import androidx.room.Embedded
import androidx.room.Relation

data class GateCodeAndApartment(
    @Embedded val gateCode: GateCode,
    @Relation(
        parentColumn = "id",
        entityColumn = "gateCodeId"
    )
    val apartment: Apartment
)
