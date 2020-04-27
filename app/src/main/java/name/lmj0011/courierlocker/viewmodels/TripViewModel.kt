package name.lmj0011.courierlocker.viewmodels

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.mooveit.library.Fakeit
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.database.Stop
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.database.TripDao
import name.lmj0011.courierlocker.helpers.Util
import name.lmj0011.courierlocker.helpers.isTripOfMonth
import name.lmj0011.courierlocker.helpers.isTripOfToday
import name.lmj0011.courierlocker.helpers.setTripTimestamp
import org.json.JSONException
import timber.log.Timber
import java.net.URLEncoder

class TripViewModel(
    val database: TripDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication())

    private val googleApiKey = preferences.getString("advancedDirectionsApiKey", "")!!

    var trips = database.getAllTrips()

    val trip = MutableLiveData<Trip?>()

    var payAmountValidated = MutableLiveData<Boolean?>()

    val totalMoney: String
        get() {
            val result = trips.value?.fold(0.0) { sum, trip ->
                val toAdd = trip.payAmount.toDoubleOrNull()
                if (toAdd == null){
                    sum
                } else {
                    sum + toAdd
                }
            }

            result?.let{
                return Util.numberFormatInstance.format(it)
            }

            return Util.numberFormatInstance.format(0.0)
        }

    val todayTotalMoney: String
        get() {
            val result = trips.value?.fold(0.0) { sum, trip ->
                val toAdd = trip.payAmount.toDoubleOrNull()
                if (toAdd == null || !isTripOfToday(trip)){
                    sum
                } else {
                    sum + toAdd
                }
            }

            result?.let{
                return Util.numberFormatInstance.format(it)
            }

            return Util.numberFormatInstance.format(0.0)
        }

    val todayCompletedTrips: String
        get() {
            val result = trips.value?.fold(0) { sum, trip ->
                if (isTripOfToday(trip) &&
                    trip.pickupAddress.isNotEmpty() &&
                    trip.dropOffAddress.isNotEmpty()
                        ){
                    sum + 1
                } else {
                    sum
                }
            }

            result?.let{
                return it.toString()
            }

            return "0"
        }

    val monthTotalMoney: String
        get() {
            val result = trips.value?.fold(0.0) { sum, trip ->
                val toAdd = trip.payAmount.toDoubleOrNull()
                if (toAdd == null || !isTripOfMonth(trip)){
                    sum
                } else {
                    sum + toAdd
                }
            }

            result?.let{
                return Util.numberFormatInstance.format(it)
            }

            return Util.numberFormatInstance.format(0.0)
        }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun setTrip(idx: Int) {
        uiScope.launch {

            val trip = withContext(Dispatchers.IO){
                this@TripViewModel.database.get(idx.toLong())
            }

            /// backwards compatibility for database v3 and lower
            trip?.let {
                when {
                    it.stops != null && it.stops.size > 0 -> {
                        // all good, this Trip was created "after" database v3
                    }
                    else -> {
                        // add pickup and dropOff address to empty Stops List for backwards compatibility
                        val pickup = Stop(it.pickupAddress, it.pickupAddressLatitude, it.pickupAddressLongitude)
                        val dropOff = Stop(it.dropOffAddress, it.dropOffAddressLatitude, it.dropOffAddressLongitude)
                        it.stops.addAll(arrayOf(pickup, dropOff))
                    }
                }
            }
            ///

            this@TripViewModel.trip.postValue(trip)
        }
    }

    fun insertTrip(
        pickupAddress: String,
        pickupAddressLatitude: Double,
        pickupAddressLongitude: Double,
        dropOffAddress: String?,
        dropOffAddressLatitude: Double?,
        dropOffAddressLongitude: Double?,
        payAmount: String?,
        gigName: String,
        stops: Array<Stop>

    ) {
        uiScope.launch {
            val trip = Trip().apply {
                setTripTimestamp(this)
                this.pickupAddress = pickupAddress
                this.pickupAddressLatitude = pickupAddressLatitude
                this.pickupAddressLongitude = pickupAddressLongitude
                dropOffAddress?.let { this.dropOffAddress = it }
                dropOffAddressLatitude?.let { this.dropOffAddressLatitude = it }
                dropOffAddressLongitude?.let { this.dropOffAddressLongitude = it }
                payAmount?.let { this.payAmount = payAmount }
                this.gigName = gigName
                this.stops.addAll(stops)
            }


            withContext(Dispatchers.IO){
                trip.distance = this@TripViewModel.setTripDistance(trip)
                this@TripViewModel.database.insert(trip)
            }
        }

    }

    fun updateTrip(trip: Trip?) {
        trip?.let {
            val stop = it.stops.last()
            it.dropOffAddress = stop.address
            it.dropOffAddressLatitude = stop.latitude
            it.dropOffAddressLongitude = stop.longitude
        }

        uiScope.launch {
            withContext(Dispatchers.IO){
                trip?.let {
                    it.distance = this@TripViewModel.setTripDistance(it)
                    this@TripViewModel.database.update(it)
                }
            }
        }

    }

    fun deleteTrip(idx: Long) {
        uiScope.launch {
            withContext(Dispatchers.IO){
                this@TripViewModel.database.deleteByTripId(idx)
            }
        }
    }


    fun insertNewRandomTripRow() {
        uiScope.launch {
            val trip = Trip(
                pickupAddress= "${Fakeit.address().streetAddress()}",
                dropOffAddress= "${Fakeit.address().streetAddress()}",
                payAmount = (1..40).random().toString(),
                gigName = "Doordash"
            ).apply {
                setTripTimestamp(this)
            }

            withContext(Dispatchers.IO){
                this@TripViewModel.database.insert(trip)
            }

        }
    }

    /**
     * Deletes all Trips permanently
     */
    fun clearAllTrips() {
        uiScope.launch {
            withContext(Dispatchers.IO){
                this@TripViewModel.database.clear()
            }

        }
    }

    fun setTripDistance(trip: Trip?): Double {
        val defaultValue = 0.0
        if (trip == null || googleApiKey.isNullOrBlank()) return defaultValue

        val sb = StringBuilder()
        // TODO will need to add waypoints for "Stops" https://developers.google.com/maps/documentation/directions/intro#Waypoints
        sb.append("https://maps.googleapis.com/maps/api/directions/json")
        sb.append("?mode=driving")
        sb.append("&origin=${trip.pickupAddressLatitude},${trip.pickupAddressLongitude}")
        sb.append("&destination=${trip.dropOffAddressLatitude},${trip.dropOffAddressLongitude}")
        sb.append("&waypoints=${generateWaypointsQueryStr(trip.stops)}")
        sb.append("&key=$googleApiKey")

        val (request, response, result) = sb.toString()
            .httpGet()
            .responseJson()

        return when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                Timber.e("Result.Failure: ${ex.message}")
                return defaultValue
            }
            is Result.Success -> {
                try {
                    var totalDistance = 0.toDouble()
                    val data = result.value.obj()
                    val route = data.getJSONArray("routes").getJSONObject(0)
                    val legs = route.getJSONArray("legs")

                    for (i in 0 until legs.length()) {
                        val leg = legs.getJSONObject(i)
                        totalDistance += leg.getJSONObject("distance").getDouble("value")
                    }

                    return totalDistance
                } catch (ex: JSONException) {
                    Timber.e("JSONException: ${ex.message}")
                }

                return defaultValue
            }
        }
    }

    fun validatePayAmount(amount: String?): Boolean {
       return try {
            amount!!.toDouble()
            payAmountValidated.value = true
            true
        } catch (ex: Exception) {
            payAmountValidated.value = false
            false
        }
    }

    private fun generateWaypointsQueryStr(stops: MutableList<Stop>): String {
        // drop the first and last Stops, since those are already used for "origin" and "destination"
        val stops2 = stops.drop(1).dropLast(1)

        val str = stops2.fold(StringBuilder()) { sb, ele ->
            sb.append("${ele.latitude},${ele.longitude}|")
        }.toString()

        return URLEncoder.encode(str, "utf-8")
    }

}