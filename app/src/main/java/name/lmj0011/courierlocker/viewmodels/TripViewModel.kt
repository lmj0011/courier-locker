package name.lmj0011.courierlocker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import androidx.paging.toLiveData
import androidx.preference.PreferenceManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.mooveit.library.Fakeit
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.Stop
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.database.TripDao
import name.lmj0011.courierlocker.helpers.*
import name.lmj0011.courierlocker.helpers.Util
import timber.log.Timber
import java.net.URLEncoder

class TripViewModel(
    val database: TripDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.IO +  viewModelJob)

    private val googleApiKey = when(PreferenceManager.getDefaultSharedPreferences(application).getBoolean("googleDirectionsKey", false)) {
        true -> application.resources.getString(R.string.google_directions_key)
        else -> ""
    }

    var filterText = MutableLiveData<String>().apply { postValue(null) }

    var errorMsg = MutableLiveData("")

    var tripPayAmountsForToday = database.getAllTodayTripPayAmounts()

    var tripPayAmountsForMonth = database.getAllMonthTripPayAmounts()

    var tripPayAmounts: LiveData<List<String>> = database.getAllTripPayAmounts()

    var tripsPaged: LiveData<PagedList<Trip>> = Transformations.switchMap(filterText) { query ->
        return@switchMap if (query.isNullOrEmpty()) {
            database.getAllTripsByThePage().toLiveData(pageSize = Const.DEFAULT_PAGE_COUNT)
        } else {
            database.getAllTripsByThePageFiltered("%$query%").toLiveData(pageSize = Const.DEFAULT_PAGE_COUNT)
        }
    }

    val trip = MutableLiveData<Trip?>()

    var payAmountValidated = MutableLiveData<Boolean?>()

    var trips: LiveData<MutableList<Trip>> = database.getAllTrips()

    val totalMoney: String
        get() {
            val result = tripPayAmounts.value?.fold(0.0) { sum, pa ->
                val toAdd = pa.toDoubleOrNull()
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
            val result = tripPayAmountsForToday.value?.fold(0.0) { sum, pa ->
                val toAdd = pa.toDoubleOrNull()
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

    val todayCompletedTrips: String
        get() {
            val result = trips.value?.fold(0) { sum, trip ->
                if (trip == null || !Util.isTripOfToday(trip) || trip.pickupAddress.isEmpty() || trip.dropOffAddress.isEmpty()){
                    sum
                } else {
                    sum + 1
                }

            }

            return result.toString()
        }

    val monthTotalMoney: String
        get() {
            val result = tripPayAmountsForMonth.value?.fold(0.0) { sum, pa ->
                val toAdd = pa.toDoubleOrNull()
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


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun setTrip(idx: Int) {
        uiScope.launch {

            val trip = this@TripViewModel.database.get(idx.toLong())

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
                Util.setTripTimestamp(this)
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

            trip.distance = this@TripViewModel.calculateTripDistance(trip)
            this@TripViewModel.database.insert(trip)
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
            trip?.let {
                try {
                    it.distance = this@TripViewModel.calculateTripDistance(it)
                    this@TripViewModel.database.update(it)
                } catch (ex: Exception) {
                    this@TripViewModel.errorMsg.postValue("distance calculation error!")
                    this@TripViewModel.database.update(it)
                    Timber.e(ex)
                }
            }
        }

    }

    fun deleteTrip(idx: Long) {
        uiScope.launch {
            this@TripViewModel.database.deleteByTripId(idx)
        }
    }

    fun insertNewRandomTripRow() {
        uiScope.launch {
            val trip = Trip(
                pickupAddress= "${Fakeit.address().streetAddress()}",
                dropOffAddress= "${Fakeit.address().streetAddress()}",
                payAmount = (1..40).random().toString(),
                gigName = arrayOf("Doordash", "Grubhub", "Postmates", "UberEats", "Lyft", "Uber", "Roadie" ).random()
            ).apply {
                Util.setTripTimestamp(this)
            }

            this@TripViewModel.database.insert(trip)

        }
    }

    /**
     * Deletes all Trips permanently
     */
    fun clearAllTrips() {
        uiScope.launch {
            this@TripViewModel.database.clear()

        }
    }

    fun calculateTripDistance(trip: Trip?): Double {
        val defaultValue = 0.0
        if (trip == null || googleApiKey.isNullOrBlank()) return defaultValue

        val sb = StringBuilder()
        // TODO will need to add waypoints for "Stops" https://developers.google.com/maps/documentation/directions/intro#Waypoints
        sb.append("https://maps.googleapis.com/maps/api/directions/json")
        sb.append("?mode=driving")
        sb.append("&origin=${trip.pickupAddressLatitude},${trip.pickupAddressLongitude}")
        sb.append("&destination=${trip.dropOffAddressLatitude},${trip.dropOffAddressLongitude}")
        generateWaypointsQueryStr(trip.stops).let {
            if(it.isNotBlank()) sb.append("&waypoints=$it")
        }
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
                var totalDistance = defaultValue
                val data = result.value.obj()
                val route = data.getJSONArray("routes").getJSONObject(0)
                val legs = route.getJSONArray("legs")

                for (i in 0 until legs.length()) {
                    val leg = legs.getJSONObject(i)
                    totalDistance += leg.getJSONObject("distance").getDouble("value")
                }

                return totalDistance
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