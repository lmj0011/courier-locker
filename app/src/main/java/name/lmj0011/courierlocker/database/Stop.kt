package name.lmj0011.courierlocker.database

// NOTE not a DB entity
data class Stop(val address: String = "", val latitude: Double = 0.toDouble(), val longitude: Double = 0.toDouble())