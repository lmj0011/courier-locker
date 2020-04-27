package name.lmj0011.courierlocker.helpers

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import timber.log.Timber
import java.io.IOException
import java.lang.Math.toRadians
import java.util.*
import kotlin.math.*

object LocationHelper {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
    private val locationRequest: LocationRequest = LocationRequest()
    private const val AVERAGE_RADIUS_OF_EARTH_KM = 6371.0 // km
    private const val AVERAGE_RADIUS_OF_EARTH_MILES = 3958.8 // mi

    var lastLatitude = MutableLiveData<Double>().apply { value = 0.0 }
        private set

    var lastLongitude = MutableLiveData<Double>().apply { value = 0.0 }
        private set

    init {
        locationRequest.interval = 2000 // milliseconds
        locationRequest.fastestInterval = 2000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    fun performAddressAutoComplete(addressStr: String, adapter: AddressAutoSuggestAdapter, job_: Job?, scope: CoroutineScope) {
        var job = job_
        // ref: https://stackoverflow.com/a/58282972/2445763
        job?.cancel()
        job = scope.launch {
            delay(500)
            if (addressStr.isNullOrEmpty().not()){
                val geolocation = GeoLocation.fromDegrees(LocationHelper.lastLatitude.value!!, LocationHelper.lastLongitude.value!!)
                val boundingBox = geolocation.boundingCoordinates(25.toDouble(), AVERAGE_RADIUS_OF_EARTH_MILES) // numbers are in miles

                try {
                    var addresses = withContext(Dispatchers.IO) {
                        getGeocoder().getFromLocationName(
                            addressStr,
                            3,
                            boundingBox[0].latitudeInDegrees,
                            boundingBox[0].longitudeInDegrees,
                            boundingBox[1].latitudeInDegrees,
                            boundingBox[1].longitudeInDegrees
                        )
                    }

                    adapter.setData(addresses)
                    adapter.notifyDataSetChanged()

                } catch (e: IOException) {
                    when (e.message) {
                        "grpc failed" -> {
                            LocationHelper.showNoGpsMessage(adapter.context)
                        }
                        else -> throw e
                    }
                }
            }
        }
    }

    /*
     Can only be set once
    */
    fun setFusedLocationClient(context: Context){
        if(this::fusedLocationClient.isInitialized) return
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        geocoder = Geocoder(context, Locale.getDefault())
    }

    private fun showNoGpsMessage(c: Context) {
        val toast = Toast.makeText(c,"GPS signal lost",Toast.LENGTH_LONG)
        toast.setGravity(Gravity.TOP, 0, 0)
        toast.show()
    }

    private fun getGeocoder(): Geocoder {
        isFusedLocationClientSet()
        return geocoder
    }

    fun getFromLocation(v: View?, latitude: Double, longitude: Double, results: Int): List<Address> {
        return try {
          geocoder.getFromLocation(latitude, longitude, results)
        } catch (ex: IOException) {
            when{
                (v != null && ex.message == "grpc failed") -> {
                    showNoGpsMessage(v.context)
                    listOf()
                }
                else -> throw ex
            }
        }
    }

    fun startLocationUpdates() {
        isFusedLocationClientSet()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    /**
     * ref: https://stackoverflow.com/a/12600225/2445763
     */
    fun calculateApproxDistanceBetweenMapPoints(
        fromLat:Double,
        fromLng:Double,
        toLat:Double,
        toLng:Double): Double {
        val latDistance = toRadians(fromLat - toLat)
        val lngDistance = toRadians(fromLng - toLng)
        val a = (sin(latDistance / 2) * sin(latDistance / 2) + (cos(toRadians(fromLat)) * cos(
            toRadians(toLat)
        ) * sin(lngDistance / 2) * sin(lngDistance / 2)))

        val c = 2.toDouble() * atan2(sqrt(a), sqrt(1 - a))
        val milesMultiplier = 0.621371 // if you want the return value in kilometers, change this to 1

        return round(AVERAGE_RADIUS_OF_EARTH_KM * c) * milesMultiplier
    }

    private fun isFusedLocationClientSet() {
        if (!this::fusedLocationClient.isInitialized) throw Exception("fusedLocationClient is not initialized!")
    }

    object locationCallback : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return

            if (locationResult.locations.isNotEmpty()) {
                // get latest location info
                lastLatitude.postValue(locationResult.lastLocation.latitude)
                lastLongitude.postValue(locationResult.lastLocation.longitude)
            }

        }
    }

}