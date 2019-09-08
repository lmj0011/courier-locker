package name.lmj0011.courierlocker.helpers

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import timber.log.Timber
import java.io.IOException
import java.util.*

object LocationHelper {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
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
        geocoder = Geocoder(context, Locale.getDefault())
    }

    fun getGeocoder(): Geocoder {
        isFusedLocationClientSet()
        return geocoder
    }

    fun getLastKnownLocation(): Task<Location>{
        isFusedLocationClientSet()
        return fusedLocationClient.lastLocation
    }

    fun startLocationUpdates() {
        isFusedLocationClientSet()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    fun stopLocationUpdates() {
        isFusedLocationClientSet()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun isFusedLocationClientSet() {
        if (!this::fusedLocationClient.isInitialized) throw Exception("fusedLocationClient is not initialized!")
    }

    object locationCallback : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return

            var addresses: List<Address> = emptyList()

            if (locationResult.locations.isNotEmpty()) {
                // get latest location
                val location = locationResult.lastLocation
            }


        }
    }

}