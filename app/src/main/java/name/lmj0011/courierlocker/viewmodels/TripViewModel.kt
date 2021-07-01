package name.lmj0011.courierlocker.viewmodels

import android.app.Application
import androidx.lifecycle.*
import androidx.paging.*
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SimpleSQLiteQuery
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.mooveit.library.Fakeit
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.Stop
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.database.TripDao
import name.lmj0011.courierlocker.helpers.*
import name.lmj0011.courierlocker.helpers.Util
import org.kodein.di.instance
import timber.log.Timber
import java.net.URLEncoder

class TripViewModel(
    val database: TripDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()
    private val preferences: PreferenceHelper = (application as CourierLockerApplication).kodein.instance()

    private val uiScope = CoroutineScope(Dispatchers.IO +  viewModelJob)

    private val googleApiKey = preferences.googleDirectionsApiKey

    var filterText = MutableLiveData<String>().apply { postValue(null) }

    var errorMsg = MutableLiveData("")

    val tripsPaged = RefreshableLiveData {
        Transformations.switchMap(filterText) { mQuery ->
            return@switchMap if (mQuery.isNullOrEmpty()) {
                Pager(
                    config = Util.getDefaultPagingConfig(),
                    initialKey = null,
                    database.getAllTripsByThePage().asPagingSourceFactory()
                ).flow.cachedIn(viewModelScope).asLiveData()
            } else {
                val query = SimpleSQLiteQuery("SELECT * FROM trips_table, json_each(stops) WHERE payAmount LIKE '%$mQuery%' OR gigName LIKE '%$mQuery%' OR json_extract(json_each.value, '\$.address') LIKE '%$mQuery%' GROUP BY trips_table.id ORDER BY trips_table.id DESC")
                Pager(
                    config = Util.getDefaultPagingConfig(),
                    initialKey = null,
                    database.getAllTripsByThePageFiltered(query).asPagingSourceFactory()
                ).flow.cachedIn(viewModelScope).asLiveData()
            }
        }
    }

    val trip = MutableLiveData<Trip?>()

    var payAmountValidated = MutableLiveData<Boolean?>()

    val trips = RefreshableLiveData { database.getAllTrips() }

    fun totalMoney(): String {
        val result = database.getAllTripPayAmounts().fold(0.0) { sum, pa ->
            val toAdd = pa.toDoubleOrNull()
            if (toAdd == null){
                sum
            } else {
                sum + toAdd
            }
        }

        return Util.numberFormatInstance.format(result)
    }

    fun todayTotalMoney(): String {
        val result = database.getAllTodayTripPayAmounts().fold(0.0) { sum, pa ->
            val toAdd = pa.toDoubleOrNull()
            if (toAdd == null){
                sum
            } else {
                sum + toAdd
            }
        }

        return Util.numberFormatInstance.format(result)
    }

    fun todayCompletedTrips(): String {
        return database.getAllTodayTrips().size.toString()
    }

    fun weekTotalMoney(): String {
        val result = database.getAllThisWeekTripPayAmounts().fold(0.0) { sum, pa ->
            val toAdd = pa.toDoubleOrNull()
            if (toAdd == null){
                sum
            } else {
                sum + toAdd
            }
        }

        return Util.numberFormatInstance.format(result)
    }

    fun monthTotalMoney(): String {
        val result = database.getAllThisMonthTripPayAmounts().fold(0.0) { sum, pa ->
            val toAdd = pa.toDoubleOrNull()
            if (toAdd == null){
                sum
            } else {
                sum + toAdd
            }
        }

        return Util.numberFormatInstance.format(result)
    }

    fun yearToDateTotalMoney(): String {
        val result = database.getAllYearToDateTripPayAmounts().fold(0.0) { sum, pa ->
            val toAdd = pa.toDoubleOrNull()
            if (toAdd == null){
                sum
            } else {
                sum + toAdd
            }
        }

        return Util.numberFormatInstance.format(result)
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
                this@TripViewModel.trip.postValue(it)
            }
            ///
        }
    }

    suspend fun getMostRecentTrips(limit: Int): List<Trip> {
        return withIOContext {
            database.getMostRecentTrips(limit)
        }
    }

    fun insertTrip(trip: Trip): Job {
        return launchIO {
            trip.apply {
                Util.setTripTimestamp(this)
                distance = calculateTripDistance(this)
            }

            database.insert(trip)
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

    ): Job {
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
        return this@TripViewModel.insertTrip(trip)
    }

    suspend fun updateTrip(trip: Trip): Trip {
        return withIOContext {
            trip.apply {
                val stop = stops.last()
                dropOffAddress = stop.address
                dropOffAddressLatitude = stop.latitude
                dropOffAddressLongitude = stop.longitude
                distance = this@TripViewModel.calculateTripDistance(this)
            }

            database.update(trip)
            return@withIOContext database.get(trip.id)!!
        }

    }

    /**
     * returns Trips within a date range.
     *
     * [startDate] and [endDate] expected to be Unix timestamps in milliseconds
     */
    fun getTripsInDateRange(startDate: Long, endDate: Long): List<Trip> {
        return this@TripViewModel.database.getAllTripsInDateRange((startDate/1000L), (endDate/1000L))
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
    suspend fun clearAllTrips(): Job {
        return launchIO { this@TripViewModel.database.clear() }
    }

    fun calculateTripDistance(trip: Trip?): Double {
        val defaultValue = 0.0
        if (trip == null || googleApiKey.isBlank()) return defaultValue

        val sb = StringBuilder()

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
                errorMsg.postValue(ex.message)
                defaultValue
            }
            is Result.Success -> {
                try {
                    var totalDistance = defaultValue
                    val data = result.value.obj()
                    val route = data.getJSONArray("routes").getJSONObject(0)
                    val legs = route.getJSONArray("legs")

                    for (i in 0 until legs.length()) {
                        val leg = legs.getJSONObject(i)
                        totalDistance += leg.getJSONObject("distance").getDouble("value")
                    }
                    totalDistance
                } catch (ex: org.json.JSONException) {
                    errorMsg.postValue(getApplication<Application>().getString(R.string.calculated_trip_error_message))
                    defaultValue
                }

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