package name.lmj0011.courierlocker.database


data class BuildingUnit(
    var number: String = "",
    var floorNumber: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var hasWaypoint: Boolean = false,
    var waypointLatitude: Double = 0.0,
    var waypointLongitude: Double = 0.0
)