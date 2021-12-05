package name.lmj0011.courierlocker.database


data class Building(
    var number: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var hasWaypoint: Boolean = false,
    var waypointLatitude: Double = 0.0,
    var waypointLongitude: Double = 0.0
)