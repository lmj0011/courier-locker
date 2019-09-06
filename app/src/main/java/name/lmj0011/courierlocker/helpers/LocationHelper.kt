package name.lmj0011.courierlocker.helpers

import android.content.Context
import com.google.android.gms.location.*
import timber.log.Timber

object LocationHelper {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequest: LocationRequest = LocationRequest()

    init {
        locationRequest.interval = 2000
        locationRequest.fastestInterval = 2000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /*
     Can only be set once
    */
    fun setFusedLocationClient(context: Context){
        if(this::fusedLocationClient.isInitialized) return
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun startLocationUpdates(locationCallback: LocationCallback) {
        if (!this::fusedLocationClient.isInitialized) throw Exception("fusedLocationClient is not initialized!")

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    fun stopLocationUpdates(locationCallback: LocationCallback) {
        if (!this::fusedLocationClient.isInitialized) throw Exception("fusedLocationClient is not initialized!")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

}