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
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.database.TripDao
import name.lmj0011.courierlocker.helpers.Util
import name.lmj0011.courierlocker.helpers.setTripTimestamp
import org.json.JSONException
import timber.log.Timber


class TripViewModel(
    val database: TripDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication())

    private val googleApiKey = preferences.getString("advancedDirectionsApiKey", "")!!

    val trips = database.getAllTrips()

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

            return Util.numberFormatInstance.format(result)
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
        gigName: String

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
            }


            withContext(Dispatchers.IO){
                trip.distance = this@TripViewModel.setTripDistance(trip)
                this@TripViewModel.database.insert(trip)
            }
        }

    }

    fun updateTrip(trip: Trip?) {
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
                dropOffAddress= "${Fakeit.address().streetAddress()}"
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

    private fun setTripDistance(trip: Trip?): Double {
        val defaultValue = 0.0
        if (trip == null || googleApiKey.isNullOrBlank()) return defaultValue

        val sb = StringBuilder()

        sb.append("https://maps.googleapis.com/maps/api/directions/json")
        sb.append("?mode=driving")
        sb.append("&origin=${trip.pickupAddressLatitude},${trip.pickupAddressLongitude}")
        sb.append("&destination=${trip.dropOffAddressLatitude},${trip.dropOffAddressLongitude}")
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
                    val data = result.value.obj()
                    val route = data.getJSONArray("routes").getJSONObject(0)
                    val leg = route.getJSONArray("legs").getJSONObject(0)
                    return leg.getJSONObject("distance").getDouble("value")
                } catch (ex: JSONException) {
                    Timber.e("JSONException: ${ex.message}")
                }

                return defaultValue
            }
        }
    }

    fun validatePayAmount(amount: String?): Boolean {
        if (amount.isNullOrEmpty()) return true

       return try {
            amount.toDouble()
            payAmountValidated.value = true
            true
        } catch (ex: NumberFormatException) {
            payAmountValidated.value = false
            false
        }
    }

}