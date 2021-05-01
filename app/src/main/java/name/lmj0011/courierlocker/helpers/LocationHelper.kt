package name.lmj0011.courierlocker.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import org.kodein.di.instance
import java.io.IOException
import java.lang.Math.toRadians
import java.util.*
import kotlin.math.*

const val AVERAGE_RADIUS_OF_EARTH_KM = 6371.0 // km
const val AVERAGE_RADIUS_OF_EARTH_MILES = 3958.8 // mi

class LocationHelper(val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val preferences: PreferenceHelper = (context.applicationContext as CourierLockerApplication).kodein.instance()
    private val geocoder = Geocoder(context, Locale.getDefault())
    private val locationRequest: LocationRequest = LocationRequest()
    private var textWatcherJob: Job = Job()

    val lastLatitude = MutableLiveData<Double>().apply { value = 0.0 }
    val lastLongitude = MutableLiveData<Double>().apply { value = 0.0 }

    init {
        locationRequest.interval = 2000 // milliseconds
        locationRequest.fastestInterval = 2000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        startLocationUpdates()
    }

    fun performAddressAutoComplete(addressStr: String, adapter: AddressAutoSuggestAdapter) {
        // ref: https://stackoverflow.com/a/58282972/2445763
        textWatcherJob.cancel()
        textWatcherJob = launchUI {
            delay(500)
            if (addressStr.isNullOrEmpty().not()){
                val geolocation = GeoLocation.fromDegrees(lastLatitude.value!!, lastLongitude.value!!)
                val boundingBox = geolocation.boundingCoordinates(preferences.boundingCoordinatesDistance, AVERAGE_RADIUS_OF_EARTH_MILES) // numbers are in miles

                try {
                    val addresses = withIOContext {
                        geocoder.getFromLocationName(
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
                            showNoGpsMessage(adapter.context)
                        }
                        else -> throw e
                    }
                }
            }
        }
    }

    private fun showNoGpsMessage(c: Context) {
        val toast = Toast.makeText(c,"GPS signal lost",Toast.LENGTH_LONG)
        toast.setGravity(Gravity.TOP, 0, 0)
        toast.show()
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

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (PermissionHelper.permissionAccessFineLocationApproved) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        locationResult?.lastLocation?.let { location ->
                            lastLatitude.postValue(location.latitude)
                            lastLongitude.postValue(location.longitude)
                        }
                    }
                },
                null /* Looper */
            )
        }
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
}