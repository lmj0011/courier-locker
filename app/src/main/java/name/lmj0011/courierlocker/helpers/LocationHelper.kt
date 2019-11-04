package name.lmj0011.courierlocker.helpers

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Handler
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
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

    var lastLatitude = MutableLiveData<Double>().apply { value = 0.0 }
        private set

    var lastLongitude = MutableLiveData<Double>().apply { value = 0.0 }
        private set

    init {
        locationRequest.interval = 2000 // milliseconds
        locationRequest.fastestInterval = 2000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    fun getNewAddressAutoCompleteHandler(adapter: AddressAutoSuggestAdapter): Handler {
        return Handler(Handler.Callback {
            val addressStr = it.data.getString("address")

            if (addressStr.isNullOrEmpty().not()){
                val geolocation = GeoLocation.fromDegrees(LocationHelper.lastLatitude.value!!, LocationHelper.lastLongitude.value!!)
                val boundingBox = geolocation.boundingCoordinates(25.toDouble(), 3958.8) // numbers are in miles

                try {
                    val addresses = this@LocationHelper.getGeocoder().getFromLocationName(
                        addressStr,
                        3,
                        boundingBox[0].latitudeInDegrees,
                        boundingBox[0].longitudeInDegrees,
                        boundingBox[1].latitudeInDegrees,
                        boundingBox[1].longitudeInDegrees
                    )

                    adapter.setData(addresses)
                    adapter.notifyDataSetChanged()

                } catch (e: IOException) {
                    when{
                        e.message == "grpc failed" -> { }
                        else -> throw e
                    }
                }
            }


            return@Callback false
        })
    }

    /*
     Can only be set once
    */
    fun setFusedLocationClient(context: Context){
        if(this::fusedLocationClient.isInitialized) return
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        geocoder = Geocoder(context, Locale.getDefault())
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
                    Snackbar.make(v, "Lost GPS Signal", Snackbar.LENGTH_LONG).show()
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

    fun stopLocationUpdates() {
        isFusedLocationClientSet()
        fusedLocationClient.removeLocationUpdates(locationCallback)
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
                lastLatitude.value  = locationResult.lastLocation.latitude
                lastLongitude.value = locationResult.lastLocation.longitude
            }

        }
    }

}